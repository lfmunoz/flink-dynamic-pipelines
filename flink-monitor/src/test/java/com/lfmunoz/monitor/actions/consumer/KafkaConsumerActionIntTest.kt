package com.lfmunoz.monitor.actions.consumer

import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.actions.producer.*
import com.lfmunoz.monitor.generateKafkaMessage
import com.lfmunoz.monitor.kafka.KafkaAdminBash
import com.lfmunoz.monitor.kafka.KafkaConfig
import com.lfmunoz.monitor.kafka.KafkaPublisherBare
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Test:  Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KafkaAdminBareIntTest {

  // Dependencies
  private val context = newFixedThreadPoolContext(4, "tThread")
  private val scope = CoroutineScope(context)
  private val aKafkaConfig = KafkaConfig()

  private val bash = BashService()
  private val kafkaAdmin = KafkaAdminBash(bash)
  private val samplingPeriod = 100L

  @Disabled
  @Test
  fun sd() {
    runBlocking {

      println(kafkaAdmin.listTopics())
      println(kafkaAdmin.listConsumerGroups())
      println(kafkaAdmin.describeTopic(aKafkaConfig.topic))
      println(kafkaAdmin.diskUsage(aKafkaConfig.topic))
      println(kafkaAdmin.describeConsumerGroup(aKafkaConfig.groupId))
    }
  }


  //________________________________________________________________________________
  // Tests
  //________________________________________________________________________________
  @Test
  fun `READ CONFIG`() {
    val action = KafkaConsumerAction()
    runBlocking {
      val resp = action.accept(buildConfigReadPkt()).toList()
      println(resp.first())
      assertThat(resp.size).isEqualTo(1)
    }
  }

  @Test
  fun `WRITE CONFIG`() {
    val configStop = KafkaConsumerConfig(isSampling = false)
    val action = KafkaConsumerAction()
    runBlocking {
      val resp = action.accept(buildConfigWritePkt(configStop)).toList()
      println(resp.first())
      assertThat(resp.size).isEqualTo(1)
    }
  }

  @Test
  fun `sample topic`() {
    val aKafkaConsumerAction = KafkaConsumerAction()
    val receivedCount = AtomicInteger(0)
    val totalMessages = 20

    // CONFIG KAFKA
    runBlocking {
      val configStop = KafkaConsumerConfig( samplingPeriod = samplingPeriod, kafkaConfig = aKafkaConfig )
      val resp = aKafkaConsumerAction.accept(buildConfigWritePkt(configStop)).toList()
      println(resp.first())
      assertThat(resp.size).isEqualTo(1)
    }

    // START SAMPLING KAFKA TOPIC
    val consumerJob = scope.launch {
      delay(1000L)
      try {
        aKafkaConsumerAction.accept(buildKafkaSamplePkt(100)).collect {
          receivedCount.getAndIncrement()
          println(it)
        }
      } catch( e: Exception) {
        e.printStackTrace()
      }
    }

    // WRITE MESSAGES TO KAFKA TOPIC
    scope.launch { kafkaProduceMessages(totalMessages) }

    await.timeout(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      assertThat(receivedCount.get()).isGreaterThanOrEqualTo(1)
    }
    stopConsumer(aKafkaConsumerAction)
    consumerJob.cancel()
  }

  //________________________________________________________________________________
  // HELPER METHODS
  //________________________________________________________________________________
  fun clearTopic() {
    runBlocking {
      println(kafkaAdmin.listTopics())
      println(kafkaAdmin.describeTopic(aKafkaConfig.topic))
//      println(kafkaAdmin.deleteTopic(aKafkaConfig.topic))
//      println(kafkaAdmin.listTopics())
//      println(kafkaAdmin.describeTopic(aKafkaConfig.topic))
    }
  }


  private fun stopConsumer(consumer: KafkaConsumerAction) {
    val configStop = KafkaConsumerConfig(isSampling = false)
    runBlocking {
      val response = consumer.accept(buildConfigWritePkt(configStop)).toList()
      assertThat(response.size).isEqualTo(1)
    }
  }

  private suspend fun kafkaProduceMessages(count: Int) {
    KafkaPublisherBare.connect(aKafkaConfig, flow {
      repeat(count) {
        emit(generateKafkaMessage(it.toLong()))
        delay(samplingPeriod/2)
      }
    }).collect { println(it) }
  }


}
