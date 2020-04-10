package com.lfmunoz.flink

//import com.c0ckp1t.app.dto.Packet
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import com.lfmunoz.flink.monitor.MonitorMessage
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

//________________________________________________________________________________
// JSON
//________________________________________________________________________________
val mapper = jacksonObjectMapper()
  .registerModule(Jdk8Module())
  .registerModule(JavaTimeModule()) // new module, NOT JSR310Module
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)

val ListOfIntType = mapper.typeFactory.constructCollectionType(List::class.java, Int::class.java)
val ListOfMonitorMessageType = mapper.typeFactory.constructCollectionType(List::class.java, MonitorMessage::class.java)
val ListOfByteArrayType = mapper.typeFactory.constructCollectionType(List::class.java, ByteArray::class.java)


fun <T: Any> String.toKotlinObject(c: KClass<T>): T {
  return mapper.readValue(this, c.java)
}

//________________________________________________________________________________
// RESULT TYPES
//________________________________________________________________________________
sealed class GenericResult<R> {
  data class Success<R>(val result: R): GenericResult<R>()
  data class Failure<R>(val message: String, val cause: Exception? = null) : GenericResult<R>()
}

//________________________________________________________________________________
// MISC
//________________________________________________________________________________

fun readTextFile(fileName: String): String {
  try {
    return Resources.getResource(fileName).readText()
  } catch (ioe: IOException) {
    throw IllegalStateException(ioe)
  }
}


