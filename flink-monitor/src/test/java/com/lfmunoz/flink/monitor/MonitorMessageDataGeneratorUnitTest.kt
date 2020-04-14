package com.lfmunoz.flink.monitor

import com.lfmunoz.flink.ListOfMonitorMessageType
import com.lfmunoz.flink.actions.producer.MonitorMessage
import com.lfmunoz.flink.actions.producer.MonitorMessageDataGenerator
import com.lfmunoz.flink.mapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorMessageDataGeneratorUnitTest {


  @Test
  fun `SERIALIZE and DESERIALIZE List Of MonitorMessage with Jackson`() {
    val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(10)
    val listOfObjects = (1..100).map {
      aMonitorMessageDataGenerator.random( 30 )
    }
    val serialize = mapper.writeValueAsBytes(listOfObjects)
    val deserialize: List<MonitorMessage> = mapper.readValue(serialize, ListOfMonitorMessageType)
    Assertions.assertThat(listOfObjects).isEqualTo(deserialize)
  }

}
