package com.lfmunoz.flink

import com.lfmunoz.flink.flink.FlinkJobContext
import com.lfmunoz.flink.flink.FlinkUtils.Companion.listByteArrayType
import com.lfmunoz.flink.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.flink.flink.ListByteArrayType
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSink
import com.lfmunoz.flink.monitor.MonitorMessage
import com.lfmunoz.flink.rabbit.rabbitSource

// Flink Job: Rabbit --> Kafka Bridge
fun rabbitToKafkaBridgeJob(jobCtx: FlinkJobContext) {
    jobCtx.env
            .rabbitSource(jobCtx.rabbitConfig)
            .map {
                return@map mapper.readValue(it, listByteArrayType) as List<ByteArray>
            }.returns(ListByteArrayType)
            .flatMap<KafkaMessage> { aByteArrayList, collector ->
                aByteArrayList.forEach { aByteArray ->
                    val aMonitorMessage = mapper.readValue(aByteArray, MonitorMessage::class.java)
                    val key = aMonitorMessage.id.toByteArray()
                    val value = mapper.writeValueAsBytes(aMonitorMessage)
                    collector.collect(KafkaMessage(key, value))
                }
            }.returns(KafkaMessage::class.java)
            .kafkaSink(
                    jobCtx.kafkaConfig
            )
    jobCtx.env.execute("[Monitor Message] Rabbit to Kafka Bridge")
}


