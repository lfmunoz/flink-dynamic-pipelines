package com.lfmunoz.monitor.kafka

import com.lfmunoz.monitor.changeLogLevel
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
  private val testThreadPool = newFixedThreadPoolContext(4, "consumerThread")

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
  }

  @Test
  @Timeout(3)
  fun `wwtf simple publish and consume`() {
    println("A")
    runBlocking {
      println("B")
        launch(testThreadPool) {
          println("C")
//        throw RuntimeException("wtf")
          Thread.sleep(15_000L)
            println("X")

      }

      val latch = CountDownLatch(1)
      assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
//      println("D")
//      if(!latch.await(2, TimeUnit.SECONDS)) {
//        println("E")
//        fail("a failing test");
//        throw RuntimeException("test failed")
//      } else {println("F")}
    }

  }
  //________________________________________________________________________________
  // Tests
  //________________________________________________________________________________
  @Test
  @Timeout(10)
  fun `simple publish and consume`() {
    val totalMessages = 1_0
    val latch = CountDownLatch(totalMessages)
    val atomicId = AtomicLong()
    val isLooping = AtomicBoolean(true)
    val aKafkaConfig = KafkaConfig()

    runBlocking {

      // CONSUMER
      launch(testThreadPool) {
        val flow = KafkaConsumerBare.connect(aKafkaConfig, isLooping )
        flow.take(totalMessages).collect {
          println(it)
          latch.countDown()
          if(latch.count == 0L) {
           isLooping.set(false)
          }
        }
      }

      // PRODUCER
      launch(testThreadPool) {
        KafkaPublisherBare.connect(aKafkaConfig, flow {
          repeat(totalMessages) {
            emit(generateKafkaMessage(atomicId.getAndIncrement()))
          }
        })
      }

      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
    }
  }

  @Test
  @Timeout(10)
  fun `multi-thread publish and consume`() {
    val totalMessages = 10_00
    val parallelism = 2
    val latch = CountDownLatch(totalMessages)
    val atomicId = AtomicLong()
    val aKafkaConfig = KafkaConfig()
    val isLooping = AtomicBoolean(true)

    runBlocking {
      val start = System.currentTimeMillis()

      // CONSUMER
        launch(testThreadPool) {
          val flow = KafkaConsumerBare.connect(aKafkaConfig, isLooping)
          flow.take(totalMessages).collect {
            println("[${latch.count}] - $it ")
            latch.countDown()
            if(latch.count == 0L) {
              isLooping.set(false)
            }
          }
        }

      // PRODUCER
      repeat(parallelism) {id ->
        launch(testThreadPool) {
          KafkaPublisherBare.connect(aKafkaConfig, flow {
            repeat(totalMessages/parallelism) {
              println("[id=${id}, count=${it}] - emit")
              emit(generateKafkaMessage(atomicId.getAndIncrement()))
            }
          })
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
  private fun generateKafkaMessage(id: Long): KafkaMessage {
    val key = id.toString().toByteArray()
    val value = "[$id] - Hello ".toByteArray()
    return KafkaMessage(key, value)
  }

  private fun printResults(total: Int, diffMillis: Long) {
    println("--------------------------------------------------------------------------")
    println("Results: ")
    println("--------------------------------------------------------------------------")
    println("Published $total in $diffMillis milliseconds  (rate = ${total.toDouble() / diffMillis})")
    println()
  }

}



