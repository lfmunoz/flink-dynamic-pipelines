package com.lfmunoz.flink.flink

import com.lfmunoz.flink.monitor.MonitorMessage
import com.lfmunoz.flink.monitor.MonitorMessageDataGenerator
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

//________________________________________________________________________________
// Int GENERATOR
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


//________________________________________________________________________________
// MonitorMessage GENERATOR
//________________________________________________________________________________
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
            val aMonitorMessage = aMonitorMessageDataGenerator.random(20)
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
