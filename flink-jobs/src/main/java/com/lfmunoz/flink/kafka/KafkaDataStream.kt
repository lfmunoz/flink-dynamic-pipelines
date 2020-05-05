package com.lfmunoz.flink.kafka

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema
import org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

//________________________________________________________________________________
// Kafka Consumer
//________________________________________________________________________________
fun StreamExecutionEnvironment.kafkaSource(
  kafkaConfig: KafkaConfig
) : DataStreamSource<KafkaMessage> {
  var properties = Properties()
  properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServer)
  properties.setProperty("group.id", "myflinkservice_${1}")
  properties.setProperty("auto.offset.reset", "earliest") // "smallest", not "earliest"
  return this.addSource( FlinkKafkaConsumer<KafkaMessage>(
    kafkaConfig.kafkaTopic,
    kafkaDeserializer,
    properties
  ), "kafkaConsumer:${kafkaConfig}")
}

val kafkaDeserializer= object : KafkaDeserializationSchema<KafkaMessage> {
  override fun deserialize(record: ConsumerRecord<ByteArray, ByteArray>): KafkaMessage {
    return KafkaMessage(record.key(), record.value())
  }
  override fun isEndOfStream(nextElement: KafkaMessage): Boolean { return false }

  override fun getProducedType(): TypeInformation<KafkaMessage> {
    return getForClass(KafkaMessage::class.java)
  }
}

//________________________________________________________________________________
// Kafka Publisher
//________________________________________________________________________________
fun  DataStream<KafkaMessage>.kafkaSink(
  kafkaConfig: KafkaConfig
) : DataStreamSink<KafkaMessage> {
  return this.addSink( FlinkKafkaProducer<KafkaMessage>(
    kafkaConfig.bootstrapServer,
    kafkaConfig.kafkaTopic,
    kafkaMessageSerializer
  )).name("kafkaPublisher:${kafkaConfig.kafkaTopic}")
}

fun SingleOutputStreamOperator<KafkaMessage>.kafkaSink(
  kafkaConfig: KafkaConfig
) : DataStreamSink<KafkaMessage> {
  val properties = Properties()
  properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServer)
//    properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4")
  return this.addSink( FlinkKafkaProducer<KafkaMessage>(
    kafkaConfig.kafkaTopic,
    kafkaMessageSerializer,
    properties
  )).name("kafkaPublisher:${kafkaConfig.kafkaTopic}")
}



val kafkaMessageSerializer = object:  KeyedSerializationSchema<KafkaMessage> {
  override fun serializeKey(element: KafkaMessage): ByteArray { return element.key }
  override fun serializeValue(element: KafkaMessage): ByteArray { return element.value }
  override fun getTargetTopic(element: KafkaMessage): String? { return null }
}
