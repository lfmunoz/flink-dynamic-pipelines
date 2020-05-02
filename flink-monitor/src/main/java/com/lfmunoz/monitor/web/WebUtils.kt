package com.lfmunoz.flink.web

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import kotlinx.coroutines.flow.Flow
import java.time.Instant

// ________________________________________________________________________________
// ACTION INTERFACE
// ________________________________________________________________________________
interface ActionInterface {
  fun accept(wsPacket: WsPacket)  : Flow<String>
}



// ________________________________________________________________________________
// CONFIG OBJECT
// ________________________________________________________________________________
data class AppConfig (
 val httpPort: Int = 8080,
 val wsPort: Int = 1991
)

// ________________________________________________________________________________
// STATIC METHODS
// ________________________________________________________________________________
fun buildWsPacketResponse(reqWsPacket: WsPacket, responsePayload: String): String {
  return WsPacket(
    payload = responsePayload,
    id = reqWsPacket.id,
    action = reqWsPacket.action,
    code = WsPacketCode.ACK
  ).toJson()
}

fun buildWsPacketError(reqWsPacket: WsPacket, errorPayload: String): String {
  return WsPacket(
    payload = errorPayload,
    id = reqWsPacket.id,
    action = reqWsPacket.action,
    code = WsPacketCode.ERROR
  ).toJson()
}

fun buildWsPacketFack(reqWsPacket: WsPacket): String {
  return WsPacket(
    payload = Instant.now().toEpochMilli().toString(),
    id = reqWsPacket.id,
    action = reqWsPacket.action,
    code = WsPacketCode.FACK
  ).toJson()
}

fun buildWsPacketLack(reqWsPacket: WsPacket): String {
  return WsPacket(
    payload = Instant.now().toEpochMilli().toString(),
    id = reqWsPacket.id,
    action = reqWsPacket.action,
    code = WsPacketCode.LACK
  ).toJson()
}

fun buildWsPacketAuthLack(requestWsPacket: WsPacket): String {
  return WsPacket(
    payload = "",
    id = requestWsPacket.id,
    action = WsPacketType.AUTH,
    code = WsPacketCode.LACK
  ).toJson()
}

// ________________________________________________________________________________
// WEBSOCKET MESSAGE
// ________________________________________________________________________________
data class WsPacket(
  val action: WsPacketType,
  val payload: String = "",
  val id: Int = 0,
  val code: WsPacketCode = WsPacketCode.LACK
) {
  // Serialize / Deserialize
  companion object {
    fun fromJson(json: String) = mapper.readValue<WsPacket>(json)
  }

  fun toJson() : String =  mapper.writeValueAsString(this)
}

enum class WsPacketType(val id: Int) {
  AUTH (1),
  TEST (2),

  KAFKA_ADMIN(3),
  KAFKA_PRODUCER(4),
  KAFKA_CONSUMER(5),
  MAPPER(6),

  INVALID(12)
}

enum class WsPacketCode(val id: Int) {
  LREQ (1), // last request
  REQ ( 2),
  FACK ( 3), // first acknowledge
  ACK ( 4),
  LACK ( 5), // last acknowledge
  ERROR( 6)
}



