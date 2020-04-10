package com.lfmunoz.flink.kafka

import ch.qos.logback.classic.Level
import com.lfmunoz.flink.changeLogLevel
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaProducerActionIntTest {

  //________________________________________________________________________________
  // BEFORE ALL / AFTER ALL
  //________________________________________________________________________________
  @BeforeAll
  fun before() {
    changeLogLevel("org.apache.kafka.clients.producer.ProducerConfig")
    changeLogLevel("org.apache.kafka.clients.consumer.ConsumerConfig")
    changeLogLevel("org.apache.kafka.common.metrics.Metrics")
    changeLogLevel("org.apache.kafka.clients.producer.KafkaProducer")
    changeLogLevel("org.apache.kafka.clients.consumer.KafkaConsumer")
    changeLogLevel("org.apache.kafka.common.utils.AppInfoParser")
    changeLogLevel("org.apache.kafka.clients.NetworkClient")
    changeLogLevel("org.apache.kafka.clients.Metadata")
    changeLogLevel("org.apache.kafka.common.network.Selector")
    changeLogLevel("com.lfmunoz.flink.kafka", Level.DEBUG)
  }

  //________________________________________________________________________________
  // TESTS
  //________________________________________________________________________________
  @Test
  fun status() {
    val action = KafkaProducerAction()
    runBlocking {
      val flow = action.accept(statusWsPacket()).toList()
      assertThat(flow.size).isEqualTo(1)
    }

  }

  @Test
  fun `start and stop producer`() {
    val action = KafkaProducerAction()
    val startMessage = KafkaActionDTO(type = KafkaActionType.START)
    val stopMessage = KafkaActionDTO(type = KafkaActionType.STOP)
    val statusMessage = KafkaActionDTO(type = KafkaActionType.STATUS)
    runBlocking {
      // START
      val startStatus = action.accept(buildWsPacket(startMessage.toJson())).map {
        return@map KafkaActionStatus.fromJson(KafkaActionDTO.fromJson(it).body)
      }.toList().first()
      assertThat(startStatus.isProducing).isTrue()
    }

    // STATUS
    await.timeout(8, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      runBlocking {
        val status = action.accept(buildWsPacket(statusMessage.toJson())).map {
          return@map KafkaActionStatus.fromJson(KafkaActionDTO.fromJson(it).body)
        }.toList().first()
        assertThat(status.isProducing).isTrue()
        assertThat(status.messagesSent).isGreaterThan(0L)
      }
    }

    // STOP
    await.timeout(8, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      runBlocking {
        val stopFlow = action.accept(buildWsPacket(stopMessage.toJson())).map {
          return@map KafkaActionStatus.fromJson(KafkaActionDTO.fromJson(it).body)
        }.toList().first()
        assertThat(stopFlow.isProducing).isFalse()
      }
    }

  }

  @Test
  fun `start and stop sampler`() {
    val atomicCount = AtomicLong(0)
    val action = KafkaProducerAction()
    val sampleMessage = KafkaActionDTO(type = KafkaActionType.SAMPLE)
    val cancelMessage = KafkaActionDTO(type = KafkaActionType.CANCEL)
    runBlocking {
      // SAMPLE
      newSingleThreadContext("sampler").use {
        launch(it) {
          action.accept(buildWsPacket(sampleMessage.toJson())).collect {
            println("[${atomicCount.getAndIncrement()}] - $it")
          }
        }
      }
      await.timeout(10, TimeUnit.SECONDS).until { atomicCount.get() > 0L }

      // CANCEL
      val cancelStatus = action.accept(buildWsPacket(cancelMessage.toJson())).map {
        return@map KafkaActionStatus.fromJson(KafkaActionDTO.fromJson(it).body)
      }.toList().first()
      assertThat(cancelStatus.messagesReceived).isGreaterThan(0L)

    }
  }

  //________________________________________________________________________________
  // HELPER METHODS
  //  ________________________________________________________________________________
  private fun statusWsPacket(): WsPacket {
    val message = KafkaActionDTO(
      type = KafkaActionType.STATUS
    )
    return buildWsPacket(message.toJson())
  }

  private fun buildWsPacket(payload: String): WsPacket {
    return WsPacket(
      action = WsPacketType.KAFKA,
      payload = payload,
      id = 10,
      code = WsPacketCode.REQ
    )
  }

}
