package sh.kma.counterwidget

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@Suppress("unused")
class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        WorkManager.initialize(context, Configuration.Builder().build())
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<CounterRepositoryLoaderWorker>().build())
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}