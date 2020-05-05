package com.lfmunoz.monitor.kafka

import com.lfmunoz.monitor.GenericResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.fissore.slf4j.FluentLoggerFactory
import java.util.*


/**
 *
 */
class KafkaPublisherBare {
  companion object {
    private val log = FluentLoggerFactory.getLogger(KafkaPublisherBare::class.java)

    suspend fun connect(aKafkaConfig: KafkaConfig, aFlow: Flow<KafkaMessage>) = flow<KafkaPublisherResult> {
      // The producer is thread safe and sharing a single producer instance across threads will generally be faster than
      // having multiple instances.
      log.info().log("[kafka producer connecting] - {}", aKafkaConfig)
      val aKafkaProducer = KafkaProducer<ByteArray, ByteArray>(producerProps(aKafkaConfig))
      try {
        aFlow.collect { item ->
          aKafkaProducer.send(ProducerRecord(aKafkaConfig.topic, item.key, item.value)) { metadata: RecordMetadata, e: Exception? ->
            e?.let {
              e.printStackTrace()
            } ?: log.trace().log("The offset of the record we just sent is: " + metadata.offset())
          }
          emit(KafkaPublisherResult.Published(item))
        }
        emit(KafkaPublisherResult.Success)
      } catch (e: Exception) {
        log.error().withCause(e).log("[kafka producer error] - captured exception")
        emit(KafkaPublisherResult.Failure( e.toString()))
      } finally {
        aKafkaProducer.close()
      }
    }

    private fun producerProps(aKafkaConfig: KafkaConfig): Properties {
      val serializer = ByteArraySerializer::class.java.canonicalName
      val props = Properties()
      props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, aKafkaConfig.bootstrapServer)
      props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer)
      props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer)
      return props
    }

  }

}// end of KafkaPublisherBare

sealed class KafkaPublisherResult {
  object Success: KafkaPublisherResult()
  data class Failure(val error: String): KafkaPublisherResult()
  data class Published(val kafkaMessage: KafkaMessage) : KafkaPublisherResult()
}
