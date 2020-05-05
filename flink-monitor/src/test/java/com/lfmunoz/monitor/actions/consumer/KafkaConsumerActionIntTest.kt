package com.lfmunoz.monitor.actions.consumer

import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.flink.web.WsPacketCode
import com.lfmunoz.flink.web.WsPacketType
import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.actions.producer.*
import com.lfmunoz.monitor.kafka.KafkaAdminBash
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Test:  Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KafkaAdminBareIntTest {

  private val topicName = "test-collect_mm"
  private val groupId = "test-collect_mm"

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
    val aKafkaProducerAction = KafkaProducerAction()
    val receivedCount = AtomicInteger(0)
    val consumerJob = GlobalScope.launch {
      aKafkaConsumerAction.accept(buildKafkaSamplePkt(100)).collect {
        receivedCount.getAndIncrement()
        println(it)
      }
    }
    val (producerJob, sentCount) = startProducer(aKafkaProducerAction)
    await.timeout(8, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      assertThat(sentCount.get()).isGreaterThanOrEqualTo(3)
    }
    await.timeout(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted {
      assertThat(receivedCount.get()).isGreaterThanOrEqualTo(3)
    }
    stopProducer(aKafkaProducerAction)
    stopConsumer(aKafkaConsumerAction)
    producerJob.cancel()
    consumerJob.cancel()
  }

  private fun stopConsumer(consumer: KafkaConsumerAction) {
    val configStop = KafkaConsumerConfig(isSampling = false)
    runBlocking {
      val response = consumer.accept(buildConfigWritePkt(configStop)).toList()
      assertThat(response.size).isEqualTo(1)
    }
  }


  fun startProducer(producer: KafkaProducerAction): Pair<Job, AtomicInteger> {
    val atomic = AtomicInteger(0)
    val job = GlobalScope.launch {
      producer.accept(startPkt()).collect {
        atomic.getAndIncrement()
        println(it)
      }
    }
    return Pair(job, atomic)
  }

  fun stopProducer(producer: KafkaProducerAction) {
    val configStop = KafkaProducerConfig(isProducing = false)
    runBlocking {
      val stopStatus = producer.accept(configWritePkt(configStop)).toList()
      assertThat(stopStatus.size).isGreaterThanOrEqualTo(1)
    }
  }


}
