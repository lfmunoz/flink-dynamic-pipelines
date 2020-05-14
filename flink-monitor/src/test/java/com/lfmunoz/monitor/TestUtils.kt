package com.lfmunoz.monitor

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.lfmunoz.monitor.kafka.KafkaMessage
import org.slf4j.LoggerFactory

fun changeLogLevel(path: String, level: Level = Level.WARN) {
  val logger = LoggerFactory.getLogger(path) as Logger
  logger.setLevel(level)
}


fun generateKafkaMessage(id: Long): KafkaMessage {
  val key = id.toString().toByteArray()
  val value = "[$id] - Hello ".toByteArray()
  return KafkaMessage(key, value)
}
