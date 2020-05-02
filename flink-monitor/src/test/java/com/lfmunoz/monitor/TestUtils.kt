package com.lfmunoz.monitor

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

fun changeLogLevel(path: String, level: Level = Level.WARN) {
  val logger = LoggerFactory.getLogger(path) as Logger
  logger.setLevel(level)
}

