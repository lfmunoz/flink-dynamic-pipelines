package com.lfmunoz.flink.kafka

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.ClassRule
import java.util.ArrayList
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import com.lfmunoz.flink.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.flink.flink.parseParameters
import com.lfmunoz.flink.kafka.KafkaConfig
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSource
import org.awaitility.kotlin.await


/**
 * Integration Test - Rabbit Integration Test
 *  Dependencies: Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaIntTest {
    @ClassRule
    var flinkCluster = MiniClusterWithClientResource(
            MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(2)
                    .setNumberTaskManagers(1)
                    .build())

    private val bootstrapServer =  System.getProperty("bootstrapServer", "localhost:9092")

    //________________________________________________________________________________
    // Test Cases
    //________________________________________________________________________________
    @Test
    fun `SERIALIZE and DESERIALIZE`() {
        val type = mapper.getTypeFactory().constructCollectionType(List::class.java, Int::class.java)
        val listOfIntegers = listOf(1, 2, 3)
        val serialize = mapper.writeValueAsBytes(listOfIntegers)
        val deserialize: List<Int> = mapper.readValue(serialize, type)
        assertThat(listOfIntegers).isEqualTo(deserialize)
    }


    @Test
    fun `Integer WRITE and READ to KAFKA`() {
        val messageTotal = 100L
        val args = arrayOf("--remote", "false")
        val aFlinkJobContext = parseParameters(args)
        val aKafkaConfig = KafkaConfig(
                bootstrapServer,
                "kafka-test"
        )

        Thread() {
            // RABBIT WRITE
            aFlinkJobContext.env.setParallelism(1)
                    .addSource(FlinkIntGenerator(messageTotal, 10L), "GradeGenerator")
                    .map {
                        val valueAndKey = mapper.writeValueAsBytes(it)
                        return@map KafkaMessage(valueAndKey, valueAndKey)
                    }.returns(KafkaMessage::class.java)
                    .kafkaSink(aKafkaConfig)

            // RABBIT READ
            aFlinkJobContext.env.setParallelism(1)
                    .kafkaSource(aKafkaConfig)
                    .map {
                        val value = mapper.readValue(it.value, Int::class.java)
                        return@map value.toString().toLong()
                    }.returns(Long::class.java)
                    .addSink(CollectSink())

            aFlinkJobContext.env.execute()
        }.start()
        await.untilAsserted {
            assertThat(CollectSink.values.size).isEqualTo(messageTotal)
        }
    }

    //________________________________________________________________________________
    // Helper methods
    //________________________________________________________________________________
    // create a testing sink
    private class CollectSink : SinkFunction<Long> {
        @Synchronized
        @Throws(Exception::class)
        override fun invoke(value: Long) {
            values.add(value)
        }

        companion object {
            // must be static
            val values: MutableList<Long> = ArrayList()
        }
    }

} // EOF

