package sh.kma.counterwidget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

class CounterSerializer : Serializer<Counter> {
    override val defaultValue: Counter = Counter.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Counter {
        try {
            return Counter.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Counter, output: OutputStream) = t.writeTo(output)
}

val Context.counterDataStore: DataStore<Counter> by dataStore(
    fileName = "counter.pb",
    serializer = CounterSerializer()
)
