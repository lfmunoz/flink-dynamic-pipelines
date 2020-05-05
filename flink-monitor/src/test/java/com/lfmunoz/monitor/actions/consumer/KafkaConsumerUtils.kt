package com.lfmunoz.monitor.actions.consumer

import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import com.lfmunoz.monitor.actions.producer.KafkaProducerConfig
import com.lfmunoz.monitor.actions.producer.KafkaProducerDTO
import com.lfmunoz.monitor.actions.producer.KafkaProducerType
import com.lfmunoz.monitor.actions.producer.buildWsPacket
import kotlin.random.Random

fun buildKafkaSamplePkt(debounce: Long): WsPacket {
  val dto = KafkaConsumerDTO(type = KafkaConsumerType.SAMPLE, body = debounce.toString())
  return createWsPacketForKafka(dto)
}

fun buildConfigWritePkt(config: KafkaConsumerConfig): WsPacket {
  val dto = KafkaConsumerDTO( type = KafkaConsumerType.CONFIG_WRITE, body = config.toJson())
  return createWsPacketForKafka(dto)
}

fun buildConfigReadPkt(): WsPacket {
  val dto = KafkaConsumerDTO( type = KafkaConsumerType.CONFIG_READ, body = "")
  return createWsPacketForKafka(dto)
}

fun createWsPacketForKafka(dto: KafkaConsumerDTO): WsPacket {
  return WsPacket(
    payload = dto.toJson(),
    action = WsPacketType.KAFKA_CONSUMER,
    id = Random.nextInt(),
    code = WsPacketCode.REQ
  )
}
