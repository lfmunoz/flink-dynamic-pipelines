package eco.analytics.bridge

import com.lfmunoz.analytics.flink.FlinkJobContext
import eco.analytics.bridge.flink.*
import eco.analytics.flink.kafka.KafkaMessage
import eco.analytics.flink.kafka.kafkaSource

// Flink Job:  Kafka --> Rabbit Bridge
fun kafkaToRabbitBridgeJob(jobCtx: FlinkJobContext) {
    jobCtx.env
            .kafkaSource(
                    jobCtx.kafkaConfig,
                    jobCtx.kafkaConfig.topics.collectMonitorMessage
            ).returns(KafkaMessage::class.java)
            .map { return@map it.value }.returns(ByteArray::class.java)
            .flatMap(bufferToByteArray<ByteArray>(jobCtx.rabbitConfig.bufferSize)).returns(ByteArray::class.java)
            .rabbitSink(jobCtx.rabbitConfig)
    jobCtx.env.execute("[Monitor Message] Kafka to Rabbit Bridge")
}


