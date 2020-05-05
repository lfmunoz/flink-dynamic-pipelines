package com.lfmunoz.monitor.actions.producer


import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import java.io.Serializable
import java.util.HashMap
import java.util.Random
import java.util.stream.IntStream


/**
 * Generator for simple flat property data structure.
 *
 * @param nrOfUuids the number of UUIDs to use when generating [MonitorMessage] objects.
 */
class MonitorMessageDataGenerator (nrOfUuids: Int) {
    private val uuidList: List<String> = (0..nrOfUuids).map{ ("UUID-GENERATED-$it") }

  /**
     * Generates a [MonitorMessage] object with the given number of numeric and boolean properties.
     *
     * @param numericProperties the number of numeric properties that will be included in the object.
     * @return a new instance of [MonitorMessage]
     */
    fun random(numericProperties: Int): MonitorMessage {
        val r = Random()
        val uuid = uuidList[r.nextInt(uuidList.size)]
        val time = System.currentTimeMillis()

        // Build the element property values (note, only numeric values are provided)yer1

        val values = HashMap<String, String>()
        IntStream.rangeClosed(1, numericProperties)
                .forEach { i -> values["property.name.goes.here.numeric$i"] = (r.nextDouble() * 100 + 50).toString() }

        return MonitorMessage(
                id = "id-$uuid",
                informTime = time,
                informIntervalMillis = 15,
                values = values,
                version = 0
        )
    }
}


/**
 * Data Class - Monitor Message
 */
data class MonitorMessage(
  var id: String = "N/A",
  var informIntervalMillis: Int = 0,
  var informTime: Long = 0,
  var values: Map<String, String> = emptyMap(),
  var version: Int = 0
) : Serializable {
  companion object {
    fun fromJson(json: String) = mapper.readValue<MonitorMessage>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

