package eco.analytics.bridge

import eco.analytics.bridge.flink.*
import eco.analytics.bridge.rabbit.rabbitSink
import eco.analytics.flink.data.eco.MonitorMessage

// Flink Job: Generate Monitor Messages to Rabbit
fun rabbitPublisherJob(jobCtx: FlinkJobContext) {
    jobCtx.env.setParallelism(1)
            .addSource(FlinkMonitorMessageGenerator(0L, 10L), "GradeGenerator")
            .flatMap(bufferToByteArray<MonitorMessage>(jobCtx.rabbitConfig.bufferSize)).returns(ByteArray::class.java)
            .rabbitSink(jobCtx.rabbitConfig)
    jobCtx.env.execute("[Monitor Message] Rabbit Publisher")
}


