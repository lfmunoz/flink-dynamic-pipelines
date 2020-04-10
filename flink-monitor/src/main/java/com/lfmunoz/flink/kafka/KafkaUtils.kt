package com.lfmunoz.flink.kafka

//________________________________________________________________________________
// KAFKA CONFIG
//________________________________________________________________________________
data class KafkaConfig(
  var bootstrapServer: String = "localhost:9092",
  var consumerTopic: String = "default-topic",
  var producerTopic: String = "default-topic",
  var groupId: String = "default-groupId",
  var compression: String = "none", // none, lz4
  var consumerOffset: String ="earliest", // latest, earliest, none(use zookeper)
  var producerOffset: String ="earliest",
  var consumeCount: Int = 0 // 0 is unlimited
) : java.io.Serializable {
//  constructor(params: ParameterTool) : this(
//    params.get("bootstrapServer", "localhost:9092"),
//    params.get("kafkaTopic", "flink-kafka-publish")
//  )
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
