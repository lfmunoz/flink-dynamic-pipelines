package com.lfmunoz.monitor.actions.producer

import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import kotlin.random.Random

fun startPkt(): WsPacket {
  val dto = KafkaProducerDTO( type = KafkaProducerType.START )
  return buildWsPacket(dto)
}

fun configReadPkt(): WsPacket {
  val dto = KafkaProducerDTO( type = KafkaProducerType.CONFIG_READ )
  return buildWsPacket(dto)
}

fun configWritePkt(config:KafkaProducerConfig ): WsPacket {
  val dto = KafkaProducerDTO( type = KafkaProducerType.CONFIG_WRITE, body = config.toJson())
  return buildWsPacket(dto)
}

fun buildWsPacket(dto: KafkaProducerDTO): WsPacket {
  return WsPacket(
    action = WsPacketType.KAFKA_PRODUCER,
    payload = dto.toJson(),
    id = Random.nextInt(),
    code = WsPacketCode.REQ
  )
}
