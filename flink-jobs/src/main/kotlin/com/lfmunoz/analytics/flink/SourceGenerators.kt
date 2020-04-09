package eco.analytics.bridge.flink

import eco.analytics.bridge.flink.FlinkUtils.Companion.mapper
import eco.analytics.bridge.monitor.MonitorMessageDataGenerator
import eco.analytics.flink.data.eco.MonitorMessage
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.util.Collector
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

//________________________________________________________________________________
// GENERATORS
//________________________________________________________________________________
class FlinkIntGenerator(
        private val maxGenerationCount: Long = 0L,
        private val generationIntervalMillis: Long = 1000L
) : RichParallelSourceFunction<Int>() {

    @Volatile private var isRunning = true
    private val count: AtomicLong = AtomicLong(0L)
    private val countValue: AtomicInteger = AtomicInteger(0)

    override fun run(ctx: SourceFunction.SourceContext<Int>) {
        while(stopCondition()) {
            Thread.sleep(generationIntervalMillis)
            ctx.collect(countValue.incrementAndGet())
        }
    }

    override fun cancel() {
        isRunning = false
    }

    private fun stopCondition() : Boolean {
        return if(maxGenerationCount == 0L) {
            isRunning
        } else {
            count.incrementAndGet()
            isRunning && count.get() <= maxGenerationCount
        }
    }
}


class FlinkMonitorMessageGenerator(
        private val maxGenerationCount: Long = 0L,
        private val generationIntervalMillis: Long = 1000L
) : RichParallelSourceFunction<MonitorMessage>() {

    private val log = LoggerFactory.getLogger(FlinkMonitorMessageGenerator::class.java)

    @Volatile private var isRunning = true
    private val count: AtomicLong = AtomicLong(0L)

    override fun run(ctx: SourceFunction.SourceContext<MonitorMessage>) {
        val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(10)
        while(stopCondition()) {
            Thread.sleep(generationIntervalMillis)
            val aMonitorMessage = aMonitorMessageDataGenerator.random(true, 30, 30)
//            ctx.collect(mapper.writeValueAsBytes(aMonitorMessage))
            ctx.collect(aMonitorMessage)
        }
        log.info("[done generating messages]")
    }

    override fun cancel() {
        isRunning = false
    }

    private fun stopCondition() : Boolean {
        return if(maxGenerationCount == 0L) {
            isRunning
        } else {
            count.incrementAndGet()
            isRunning && count.get() <= maxGenerationCount
        }
    }
}



/*
class FlinkMonitorMessageGenerator(
        private val maxGenerationCount: Long = 0L,
        private val generationIntervalMillis: Long = 1000L
) : RichParallelSourceFunction<ByteArray>() {

    private val log = LoggerFactory.getLogger(FlinkMonitorMessageGenerator::class.java)

    @Volatile private var isRunning = true
    private val count: AtomicLong = AtomicLong(0L)

    override fun run(ctx: SourceFunction.SourceContext<ByteArray>) {
        val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(10)
        while(stopCondition()) {
            Thread.sleep(generationIntervalMillis)
            val aMonitorMessage = aMonitorMessageDataGenerator.random(true, 30, 30)
            ctx.collect(mapper.writeValueAsBytes(aMonitorMessage))
        }
        log.info("[done generating messages]")
    }

    override fun cancel() {
        isRunning = false
    }

    private fun stopCondition() : Boolean {
        return if(maxGenerationCount == 0L) {
            isRunning
        } else {
            count.incrementAndGet()
            isRunning && count.get() <= maxGenerationCount
        }
    }
}
*/

//________________________________________________________________________________
// MISC
//________________________________________________________________________________
fun <T> take(
        count: Long = 100L
) : RichMapFunction<T, T> {
    return object: RichMapFunction<T, T>() {
        val numberOfSeenValues: AtomicLong = AtomicLong(0)
        override fun map(value: T): T {
            if(numberOfSeenValues.incrementAndGet() > count) {
                throw RuntimeException("[take completed ] - Took ${numberOfSeenValues.get()}")
            }
            println(numberOfSeenValues.get())
            return value
        }
    }
}

