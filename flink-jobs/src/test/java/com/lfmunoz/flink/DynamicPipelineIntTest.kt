package com.lfmunoz.flink

import com.lfmunoz.flink.monitor.MonitorMessage
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.lfmunoz.flink.engine.DynamicMapperProcessFunction
import com.lfmunoz.flink.engine.MapperResult
import com.lfmunoz.flink.flink.*
import com.lfmunoz.flink.flink.FlinkUtils.Companion.mapToStringOfStringType
import com.lfmunoz.flink.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSink
import com.lfmunoz.flink.kafka.kafkaSource
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * Integration Test - Full
 *  Dependencies:  Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicPipelineIntTest {

  @ClassRule
  var flinkCluster = MiniClusterWithClientResource(
    MiniClusterResourceConfiguration.Builder()
      .setNumberSlotsPerTaskManager(2)
      .setNumberTaskManagers(1)
      .build())

  private val bootstrapServer = System.getProperty("bootstrapServer", "localhost:9092")

  //________________________________________________________________________________
  // Test Cases
  //________________________________________________________________________________
  @Test
  fun `KAFKA to MAPPER to KAFKA`() {
    val args = arrayOf(
      "--remote", "false",
      "--bootstrapServer", bootstrapServer
    )
    val jobCtx = parseParameters(args)

    Thread() {
      // PUBLISH MESSAGES TO KAFKA
      jobCtx.env.setParallelism(1)
        .addSource(FlinkMonitorMessageGenerator(0L, 200L))
        .map {
          val key = mapper.writeValueAsBytes(it.id)
          val value = mapper.writeValueAsBytes(it)
          KafkaMessage(key, value)
        }.returns(KafkaMessage::class.java)
        .kafkaSink(jobCtx.inputKafka)

      // READ CONFIG FROM KAFKA
      val broadcastMapperStream = jobCtx.env.setParallelism(1)
        .kafkaSource(jobCtx.mapperKafka)
        .map {
          println(it.value.toString())
          mapper.readValue(it.value, mapToStringOfStringType) as Map<String, String>
        }
        .returns(MapOfStringToString)
        .broadcast(mapperStateDescriptor)

      // RUN THE MAPPER PIPELINE
      jobCtx.env
        .kafkaSource(jobCtx.inputKafka)
        .map { it.value }.returns(ByteArray::class.java)
        .map { mapper.readValue(it, MonitorMessage::class.java) }.returns(MonitorMessage::class.java)
        .connect(broadcastMapperStream)
        .process(DynamicMapperProcessFunction)
        .returns(MapperResult::class.java)
        .map {
          val key = mapper.writeValueAsBytes(it.id)
          val value = mapper.writeValueAsBytes(it)
          KafkaMessage(key, value)
        }
        .returns(KafkaMessage::class.java)
        .kafkaSink(jobCtx.outputKafka)

      // READ RESULT FROM KAFKA
      jobCtx.env
        .kafkaSource(jobCtx.inputKafka)
        .map { it.value }.returns(ByteArray::class.java)
        .addSink(CollectSink())

      jobCtx.env.execute()

    }.start()

    await.timeout(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      assertThat(CollectSink.values.size).isGreaterThan(10)
    }

  }

} // EOF
