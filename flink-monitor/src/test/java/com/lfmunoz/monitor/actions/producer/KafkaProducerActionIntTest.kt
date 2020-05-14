package com.lfmunoz.monitor.actions.producer

import ch.qos.logback.classic.Level
import com.lfmunoz.monitor.changeLogLevel
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import com.lfmunoz.monitor.actions.consumer.KafkaConsumerDTO
import com.lfmunoz.monitor.actions.producer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaProducerActionIntTest {

  // Dependencies
  private val context = newFixedThreadPoolContext(4, "tThread")
  private val scope = CoroutineScope(context)

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
  fun `READ CONFIG`() {
    val action = KafkaProducerAction()
    runBlocking {
      val resp = action.accept(configReadPkt()).toList()
      assertThat(resp.size).isEqualTo(1)
    }
  }

  @Test
  fun `start and stop producer`() {
    val atomic = AtomicInteger(0)
    val action = KafkaProducerAction()
    val producerJob  = scope.launch {
    // START
      val startStatus = action.accept(startPkt()).map {
        atomic.getAndIncrement()
        println(it)
        it
      }.toList()
      assertThat(startStatus.size).isGreaterThanOrEqualTo(1)
    }

    // STOP
    val configStop = KafkaProducerConfig( isProducing = false)
    await.timeout(8, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      runBlocking {
        delay(1000)
        val stopStatus =  action.accept(configWritePkt(configStop)).map {
          println(it)
          it
        }.toList()
        assertThat(stopStatus.size).isGreaterThanOrEqualTo(1)
      }
    }
  }


}
