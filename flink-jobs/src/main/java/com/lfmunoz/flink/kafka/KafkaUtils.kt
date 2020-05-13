package com.lfmunoz.flink.kafka

import org.apache.flink.api.java.utils.ParameterTool

//________________________________________________________________________________
// KAFKA CONFIG
//________________________________________________________________________________
data class KafkaConfig(
  var bootstrapServer: String = "",
  var kafkaTopic: String = ""
) : java.io.Serializable {
  constructor(params: ParameterTool) : this(
    params.get("bootstrapServer", "localhost:9092"),
    params.get("kafkaTopic", "flink-kafka-publish")
  )
}

//________________________________________________________________________________
// KAFKA MESSAGE
//________________________________________________________________________________
data class KafkaMessage(
  var key: ByteArray,
  var value: ByteArray
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
