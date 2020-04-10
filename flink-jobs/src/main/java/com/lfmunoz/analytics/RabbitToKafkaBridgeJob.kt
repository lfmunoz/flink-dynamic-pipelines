package eco.analytics.bridge

import com.lfmunoz.analytics.flink.FlinkJobContext
import com.lfmunoz.analytics.flink.FlinkUtils.Companion.listByteArrayType
import com.lfmunoz.analytics.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.analytics.flink.ListByteArrayType
import eco.analytics.flink.kafka.KafkaMessage
import eco.analytics.flink.kafka.kafkaSink
import eco.analytics.bridge.rabbit.rabbitSource
import eco.analytics.flink.data.eco.MonitorMessage

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
                    val key = aMonitorMessage.uuid?.toByteArray() ?: "".toByteArray()
                    val value = mapper.writeValueAsBytes(aMonitorMessage)
                    collector.collect(KafkaMessage(key, value))
                }
            }.returns(KafkaMessage::class.java)
            .kafkaSink(
                    jobCtx.kafkaConfig,
                    jobCtx.kafkaConfig.topics.collectMonitorMessage
            )
    jobCtx.env.execute("[Monitor Message] Rabbit to Kafka Bridge")
}


