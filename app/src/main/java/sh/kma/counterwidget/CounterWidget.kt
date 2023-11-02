package sh.kma.counterwidget

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import java.util.logging.Logger


class CounterWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(90.dp, 90.dp)
        private val MEDIUM = DpSize(100.dp, 100.dp)
        private val LARGE = DpSize(180.dp, 120.dp)
        private val ROW = DpSize(100.dp, 90.dp)
        private val LARGE_ROW = DpSize(180.dp, 90.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL, MEDIUM, LARGE, ROW, LARGE_ROW
        )
    )
    private var context: Context? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        this.context = context
        provideContent {
            GlanceTheme {
                Content()
            }
        }
    }

    @Composable
    @Preview
    private fun Content() {
        val context = this.context
        if (context == null) {
            Logger.getGlobal().severe("No context")
            return
        }

        val repository = remember { CounterRepository.instance }

        val size = LocalSize.current
        val lowVerticalSpace = size.height <= SMALL.height
        val isCompact = size.width <= ROW.width || size.height <= ROW.height

        val containerModifier = GlanceModifier.fillMaxSize()
            .padding(if (isCompact) 0.dp else 8.dp)
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .appWidgetBackgroundCornerRadius()

        if (isCompact) {
            BoxLayout(modifier = containerModifier) {
                MainContent(repository, isCompact, lowVerticalSpace)
            }
        } else {
            ColumnLayout(modifier = containerModifier) {
                MainContent(repository, isCompact, lowVerticalSpace)
            }
        }
    }

    @Composable
    private fun ColumnLayout(
        modifier: GlanceModifier,
        content: @Composable() (ColumnScope.() -> Unit)
    ) {
        Column(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }

    @Composable
    private fun BoxLayout(modifier: GlanceModifier, content: @Composable() (() -> Unit)) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
            content = content,
        )
    }

    @Composable
    private fun MainContent(
        repository: CounterRepository,
        isCompact: Boolean,
        lowVerticalSpace: Boolean
    ) {
        val state by repository.state.collectAsState()
        val count by repository.count.collectAsState()

        var mainRowModifier = GlanceModifier
            .fillMaxWidth()
            .padding(if (isCompact) 0.dp else 8.dp)
        if (isCompact)
            mainRowModifier = mainRowModifier.fillMaxHeight()

        var buttonModifier =
            GlanceModifier.cornerRadius(32.dp).appWidgetButtonWidth(LocalSize.current).padding(8.dp)
        if (isCompact)
            buttonModifier = buttonModifier.fillMaxHeight().padding(0.dp)
        val buttonStyle =
            TextStyle(fontSize = TextUnit(24f, TextUnitType.Sp), textAlign = TextAlign.Center)

        val resetButtonModifier = GlanceModifier.padding(if (lowVerticalSpace) 4.dp else 8.dp)
        val resetButtonStyle =
            TextStyle(
                fontSize = TextUnit(if (lowVerticalSpace) 12f else 16f, TextUnitType.Sp),
                textAlign = TextAlign.Center
            )
        Row(
            modifier = if (isCompact) GlanceModifier.fillMaxSize() else GlanceModifier.fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                text = "RESET",
                onClick = actionRunCallback<ResetAction>(),
                style = resetButtonStyle,
                modifier = resetButtonModifier,
            )
        }

        Row(
            modifier = mainRowModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val text: String =
                when (state) {
                    State.Loading -> "Loading..."
                    State.Error -> "Error"
                    State.Loaded -> count.toString()
                }
            Button(
                text = "-",
                onClick = actionRunCallback<RemoveCountAction>(),
                style = buttonStyle,
                modifier = buttonModifier,
            )
            Text(
                text,
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(32f, TextUnitType.Sp),
                ),
            )
            Button(
                text = "+",
                onClick = actionRunCallback<AddCountAction>(),
                style = buttonStyle,
                modifier = buttonModifier
            )
        }
    }
}

class ResetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CounterRepository.instance.resetCount(context)
        UpdateWidgetAction().onAction(context, glanceId, parameters)
        VibrateAction().onAction(context, glanceId, parameters)
    }
}

class AddCountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CounterRepository.instance.addCount(context)
        UpdateWidgetAction().onAction(context, glanceId, parameters)
        VibrateAction().onAction(context, glanceId, parameters)
    }
}

class RemoveCountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CounterRepository.instance.removeCount(context)
        UpdateWidgetAction().onAction(context, glanceId, parameters)
        VibrateAction().onAction(context, glanceId, parameters)
    }
}

class UpdateWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CounterWidget().updateAll(context)
    }
}

class VibrateAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            val vibrator = context.getSystemService(VibratorManager::class.java)
            val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION)
            vibrator.defaultVibrator.vibrate(effect, usage)
        } else {
            val vibrator = context.getSystemService(Vibrator::class.java)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    else
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect, audioAttributes)
            } else {
                vibrator.vibrate(50, audioAttributes)
            }
        }
    }
}

fun GlanceModifier.appWidgetBackgroundCornerRadius(): GlanceModifier {
    return if (Build.VERSION.SDK_INT >= 31) {
        this.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        this.cornerRadius(16.dp)
    }
}

fun GlanceModifier.appWidgetButtonWidth(size: DpSize): GlanceModifier {
    val targetWidth = size.width / 4
    val minWidth = 16.dp * 2
    return this.width(if (targetWidth > minWidth) targetWidth else minWidth)
}
