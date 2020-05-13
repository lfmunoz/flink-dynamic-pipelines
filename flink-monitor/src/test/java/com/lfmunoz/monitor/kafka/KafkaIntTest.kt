package com.lfmunoz.monitor.kafka

import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.changeLogLevel
import com.lfmunoz.monitor.generateKafkaMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Integration Test:  Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaIntTest {

  // Dependencies
  private val context = newFixedThreadPoolContext(4, "consumerThread")
  private val scope = CoroutineScope(context)

  private val bash = BashService()
  private val kafkaAdmin = KafkaAdminBash(bash)
  private val aKafkaConfig = KafkaConfig()

  //________________________________________________________________________________
  // BEFORE ALL / AFTER ALL
  //________________________________________________________________________________
  @BeforeAll
  fun beforeAll() {
    changeLogLevel("org.apache.kafka.clients.producer.ProducerConfig")
    changeLogLevel("org.apache.kafka.clients.consumer.ConsumerConfig")
    changeLogLevel("org.apache.kafka.common.metrics.Metrics")
    changeLogLevel("org.apache.kafka.clients.producer.KafkaProducer")
    changeLogLevel("org.apache.kafka.clients.consumer.KafkaConsumer")
    changeLogLevel("org.apache.kafka.common.utils.AppInfoParser")
    changeLogLevel("org.apache.kafka.clients.NetworkClient")
    changeLogLevel("org.apache.kafka.clients.Metadata")
    changeLogLevel("org.apache.kafka.common.network.Selector")
  }

  @BeforeEach
  fun beforeEach() {


  }

  @Test
  fun kafkaTopic() {
    runBlocking {
      println(kafkaAdmin.listTopics())
      println(kafkaAdmin.deleteTopic(aKafkaConfig.topic))
      println(kafkaAdmin.listTopics())
    }
  }


  //________________________________________________________________________________
  // Tests
  //________________________________________________________________________________
  @Test
  fun `simple publish and consume`() {
    val totalMessages = 1_0
    val latch = CountDownLatch(totalMessages)
    val atomicId = AtomicLong()
    val isLooping = AtomicBoolean(true)

    runBlocking {
      // CONSUMER
      scope.launch {
        val flow = KafkaConsumerBare.connect(aKafkaConfig, isLooping )
        flow.take(totalMessages).collect {
          println(it)
          latch.countDown()
          if(latch.count == 0L) { isLooping.set(false) }
        }
      }

      // PRODUCER
      scope.launch {
        KafkaPublisherBare.connect(aKafkaConfig, flow {
          repeat(totalMessages) { emit(generateKafkaMessage(atomicId.getAndIncrement())) }
        }).collect { println(it) }
      }
      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }
  }

  @Test
  fun `multi-thread publish and consume`() {
    val totalMessages = 10_00
    val parallelism = 2
    val latch = CountDownLatch(totalMessages)
    val atomicId = AtomicLong()
    val isLooping = AtomicBoolean(true)

    runBlocking {
      val start = System.currentTimeMillis()

      // CONSUMER
        scope.launch {
          val flow = KafkaConsumerBare.connect(aKafkaConfig, isLooping)
          flow.take(totalMessages).collect {
            println("[${latch.count}] - $it ")
            latch.countDown()
            if(latch.count == 0L) { isLooping.set(false) }
          }
        }

      // PRODUCER
      repeat(parallelism) {id ->
        scope.launch {
          KafkaPublisherBare.connect(aKafkaConfig, flow {
            repeat(totalMessages/parallelism) {
              println("[id=${id}, count=${it}] - emit")
              emit(generateKafkaMessage(atomicId.getAndIncrement()))
            }
          }).collect {print(it)}
        }
      }

      assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
      val stop = System.currentTimeMillis()
      printResults(totalMessages,  stop - start)
    }
  }


  //________________________________________________________________________________
// Helper methods
//________________________________________________________________________________


  private fun printResults(total: Int, diffMillis: Long) {
    println("--------------------------------------------------------------------------")
    println("Results: ")
    println("--------------------------------------------------------------------------")
    println("Published $total in $diffMillis milliseconds  (rate = ${total.toDouble() / (diffMillis.toDouble() / 1000) } messages/s)")
    println()
  }

}



