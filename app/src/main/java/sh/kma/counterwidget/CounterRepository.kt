package sh.kma.counterwidget

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class CounterRepository private constructor() {
    companion object {
        val instance: CounterRepository by lazy {
            CounterRepository()
        }
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()
    private val _count: MutableStateFlow<Int> = MutableStateFlow(0)
    val count = _count.asStateFlow()

    suspend fun addCount(context: Context) {
        _count.update { count -> count + 1 }
        saveCounter(context)
    }

    suspend fun removeCount(context: Context) {
        _count.update { count -> count - 1 }
        saveCounter(context)
    }

    suspend fun resetCount(context: Context) {
        _count.update { 0 }
        saveCounter(context)
    }

    suspend fun load(context: Context) {
        val savedCount: Flow<Int> = context.counterDataStore.data.map { counter ->
            counter.count
        }
        _count.update { savedCount.firstOrNull() ?: 0 }
        _state.update { State.Loaded }
    }

    private suspend fun saveCounter(context: Context) {
        context.counterDataStore.updateData { counter ->
            counter.toBuilder()
                .setCount(count.value)
                .build()
        }
    }
}

enum class State {
    Loading,
    Error,
    Loaded
}
