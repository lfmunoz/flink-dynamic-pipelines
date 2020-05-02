package com.lfmunoz.monitor.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper

//________________________________________________________________________________
// KAFKA CONFIG
//________________________________________________________________________________
data class KafkaConfig(
  var bootstrapServer: String = "localhost:9092",
  var topic: String = "default-topic",
  var groupId: String = "default-groupId",
  var compression: String = "none", // none, lz4
  var offset: String ="earliest", // latest, earliest, none(use zookeper)
  var value: Int = 0
) : java.io.Serializable {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaConfig>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

//________________________________________________________________________________
// KAFKA MESSAGE
//________________________________________________________________________________
data class KafkaMessage(
  val key: ByteArray,
  val value: ByteArray
) : java.io.Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as KafkaMessage
    if (!key.contentEquals(other.key)) return false
    return true
  }

  override fun hashCode(): Int {
    return key.contentHashCode()
  }
}
