package com.lfmunoz.flink.test

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.flink.mapper
import java.time.Instant

//________________________________________________________________________________
// Entity - Test
//________________________________________________________________________________
data class TestEntity(
  var id: Long = 0L,
  var name: String = "N/A",
  var updated: Instant = Instant.now()
) {

  companion object {
    fun fromJson(json: String) = mapper.readValue<TestEntity>(json)
  }

}

//________________________________________________________________________________
// Data Transfer Object - Test
//________________________________________________________________________________
data class TestDTO(
  var id: Long,
  val created: Instant,
  val count: Int,
  val message: String
) {
  // Serialize / Deserialize
  companion object {
    fun fromJson(json: String) = mapper.readValue<TestDTO>(json)
  }

  fun toJson() : String =  mapper.writeValueAsString(this)
}

