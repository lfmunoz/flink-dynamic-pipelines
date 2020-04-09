package eco.analytics.bridge.flink

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.util.Collector


//________________________________________________________________________________
// BUFFER
//________________________________________________________________________________
fun <T> bufferToByteArray(
        bufferSize: Int = 100,
        maxInterval: Long = 30_000L
) : RichFlatMapFunction<T, ByteArray> {
    return object: RichFlatMapFunction<T, ByteArray>() {
        private lateinit var bufferedElements: List<MutableList<T>>
        private lateinit var lastTimeSentList: MutableList<Long>
        private val mapper = ObjectMapper()

        override fun flatMap(value: T, out: Collector<ByteArray>) {
            val currentTime = System.currentTimeMillis()
            val buffer = bufferedElements[runtimeContext.indexOfThisSubtask]
            val timeInterval = currentTime - lastTimeSentList[runtimeContext.indexOfThisSubtask]
            buffer.add(value)
            if(buffer.size == bufferSize || timeInterval > maxInterval ) {
                val bytesSerialize = mapper.writeValueAsBytes(buffer)
                out.collect(bytesSerialize)
                buffer.clear()
                lastTimeSentList[runtimeContext.indexOfThisSubtask] = currentTime
            }
        }

        override fun open(parameters: Configuration) {
            bufferedElements = (1..runtimeContext.maxNumberOfParallelSubtasks)
                    .map { mutableListOf<T>() }
            lastTimeSentList = (1..runtimeContext.maxNumberOfParallelSubtasks)
                    .map { System.currentTimeMillis() }.toMutableList()
        }

        override fun close() { }
    }
}

fun <T> buffer(
        bufferSize: Int = 100
) : RichFlatMapFunction<T, List<T>> {
    return object: RichFlatMapFunction<T, List<T>>() {
        private lateinit var bufferedElements: List<MutableList<T>>

        override fun flatMap(value: T, out: Collector<List<T>>) {
            val buffer = bufferedElements[runtimeContext.indexOfThisSubtask]
            if(buffer.size > bufferSize) {
                out.collect(buffer)
                buffer.clear()
            } else {
                buffer.add(value)
            }
        }

        override fun open(parameters: Configuration) {
            bufferedElements = (1..runtimeContext.maxNumberOfParallelSubtasks)
                    .map { mutableListOf<T>() }
        }

        override fun close() { }
    }
}

fun <T> buffer(
        bufferSize: Int = 100,
        compact: (List<T>) -> ByteArray
) : RichFlatMapFunction<T, ByteArray> {
    return object: RichFlatMapFunction<T, ByteArray>() {
        private lateinit var bufferedElements: List<MutableList<T>>

        override fun flatMap(value: T, out: Collector<ByteArray>) {
            val buffer = bufferedElements[runtimeContext.indexOfThisSubtask]
            if(buffer.size > bufferSize) {
                out.collect(compact(buffer))
                buffer.clear()
            } else {
                buffer.add(value)
            }
        }

        override fun open(parameters: Configuration) {
            bufferedElements = (1..runtimeContext.maxNumberOfParallelSubtasks)
                    .map { mutableListOf<T>() }
        }

        override fun close() { }
    }
}

