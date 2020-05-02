package com.lfmunoz.flink

import com.lfmunoz.flink.flink.FlinkJobContext
import com.lfmunoz.flink.flink.FlinkMonitorMessageGenerator
import com.lfmunoz.flink.flink.bufferToByteArray
import com.lfmunoz.flink.monitor.MonitorMessage
import com.lfmunoz.flink.rabbit.rabbitSink

// Flink Job: Generate Monitor Messages to Rabbit
fun rabbitPublisherJob(jobCtx: FlinkJobContext) {
    jobCtx.env.setParallelism(1)
            .addSource(FlinkMonitorMessageGenerator(0L, 10L), "GradeGenerator")
            .flatMap(bufferToByteArray<MonitorMessage>(jobCtx.rabbitConfig.bufferSize)).returns(ByteArray::class.java)
            .rabbitSink(jobCtx.rabbitConfig)
    jobCtx.env.execute("[Monitor Message] Rabbit Publisher")
}


