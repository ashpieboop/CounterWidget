package sh.kma.counterwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CounterRepositoryLoaderWorker(
    private val context: Context,
    params: WorkerParameters,
): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = CounterRepository.instance
        if (repository.state.value != State.Loaded) repository.load(context)
        CounterWidget().updateAll(context)
        return Result.success()
    }
}