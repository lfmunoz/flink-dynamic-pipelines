package eco.analytics.bridge

import com.lfmunoz.analytics.flink.FlinkJobContext
import eco.analytics.bridge.flink.*
import eco.analytics.flink.data.eco.MonitorMessage

// Flink Job: Generate Monitor Messages to Rabbit
fun rabbitPublisherJob(jobCtx: FlinkJobContext) {
    jobCtx.env.setParallelism(1)
            .addSource(FlinkMonitorMessageGenerator(0L, 10L), "GradeGenerator")
            .flatMap(bufferToByteArray<MonitorMessage>(jobCtx.rabbitConfig.bufferSize)).returns(ByteArray::class.java)
            .rabbitSink(jobCtx.rabbitConfig)
    jobCtx.env.execute("[Monitor Message] Rabbit Publisher")
}


