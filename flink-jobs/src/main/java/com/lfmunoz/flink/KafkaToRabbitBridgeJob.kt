package com.lfmunoz.flink

import com.lfmunoz.flink.flink.FlinkJobContext
import com.lfmunoz.flink.flink.bufferToByteArray
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSource
import com.lfmunoz.flink.rabbit.rabbitSink

// Flink Job:  Kafka --> Rabbit Bridge
fun kafkaToRabbitBridgeJob(jobCtx: FlinkJobContext) {
    jobCtx.env
            .kafkaSource(
                    jobCtx.kafkaConfig
            ).returns(KafkaMessage::class.java)
            .map { return@map it.value }.returns(ByteArray::class.java)
            .flatMap(bufferToByteArray<ByteArray>(jobCtx.rabbitConfig.bufferSize)).returns(ByteArray::class.java)
            .rabbitSink(jobCtx.rabbitConfig)
    jobCtx.env.execute("[Monitor Message] Kafka to Rabbit Bridge")
}


