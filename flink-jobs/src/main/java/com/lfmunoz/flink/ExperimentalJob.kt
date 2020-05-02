package com.lfmunoz.flink

import com.lfmunoz.flink.flink.FlinkIntGenerator
import com.lfmunoz.flink.flink.parseParameters
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.util.Collector
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// Flink Job: Generate Monitor Messages to Rabbit
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("FlinkMainAppKt")
    val jobCtx = parseParameters(args)
    log.info("Flink Application started")
    log.info("[config] - ${jobCtx}")

    val ruleStateDescriptor= MapStateDescriptor<String, Int>(
            "RulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            BasicTypeInfo.INT_TYPE_INFO
    )

    val dataStream = jobCtx.env.setParallelism(3)
            .addSource(FlinkIntGenerator(), "GradeGenerator")

    val broadcastStream =  jobCtx.env.setParallelism(1)
            .addSource(InfiniteMapGenerator(), "GradeScaleGenerator")
            .broadcast(ruleStateDescriptor)

    dataStream
            .connect(broadcastStream)
            .process(object: BroadcastProcessFunction<Int, Map<String, Int>, String>() {
                var currentMax = AtomicInteger(100)
                var currentMin = AtomicInteger(50)
                override fun processElement(value: Int, ctx: ReadOnlyContext, out: Collector<String>) {
                    if(value <= currentMax.get() && value >= currentMin.get()) {
                        out.collect("TRUE value=$value, MAX=$currentMax, MIN=$currentMin")
                    } else {
                        out.collect("FALSE value=$value, MAX=$currentMax, MIN=$currentMin")
                    }
                }

                override fun processBroadcastElement(value: Map<String, Int>, ctx: Context, out: Collector<String>) {
                    out.collect("------------------------------------------------------------------------")
                    currentMax.set(value["MAX"] ?: 100)
                    currentMin.set(value["MIN"] ?: 50)
                }
            })
            .print()

    jobCtx.env.execute("[Experimental Job] ")
}

data class WhiteList(
        var flinkHost: String = "",
        var flinkPort: Int = 0,
        var uberJar: String = ""
) : java.io.Serializable {

}


class InfiniteMapGenerator : RichParallelSourceFunction<Map<String, Int>>() {

    @Volatile private var isRunning = true

    private val gradesStandard = mapOf(
            "MAX" to 100,
            "MIN" to 50
    )

    override fun run(ctx: SourceFunction.SourceContext<Map<String, Int>>) {
        while(isRunning) {
            val rand = Random()
            val max = rand.nextInt(50) + 50
            val min = rand.nextInt(50)
            ctx.collect(mapOf("MAX" to max, "MIN" to min))
            Thread.sleep(5000L)
        }
    }

    override fun cancel() {
        isRunning = false
    }

}
