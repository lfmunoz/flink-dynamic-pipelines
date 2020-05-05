package com.lfmunoz.monitor.actions.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.kafka.KafkaConfig
import com.lfmunoz.monitor.kafka.KafkaConsumerBare
import com.lfmunoz.monitor.kafka.KafkaMessage
import com.lfmunoz.monitor.kafka.KafkaPublisherBare
import com.lfmunoz.monitor.mapper
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.monitor.actions.producer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class KafkaConsumerAction : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaConsumerAction::class.java)
  }

  // CONSUMER
  @get:Synchronized @set:Synchronized
  private var aKafkaConfig = KafkaConfig(groupId = "groupId-${Random.nextInt(1000,9999)}")
  private val isSampling = AtomicBoolean(false)
  private val messagesReceived = AtomicLong(0L)
  private val samplingPeriod = AtomicLong(1_000L)

  private val job = SupervisorJob()
  private val context = newFixedThreadPoolContext(3, "kafkaConsumer")
  private val scope = CoroutineScope(context + job)


  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = KafkaConsumerDTO.fromJson(wsPacket.payload)
      Log.info().log("[KafkaConsumerAction] - {}", dto)

      when (dto.type) {
        KafkaConsumerType.SAMPLE -> {
          emitAll(sampling(dto).map{ mapper.writeValueAsString(it)})
        }
        KafkaConsumerType.CONFIG_READ -> {
          emitAll(configRead(dto).map { it.toJson() })
        }
        KafkaConsumerType.CONFIG_WRITE -> {
          emitAll(configWrite(dto).map { it.toJson() })
        }
        else -> {
          emit(KafkaConsumerDTO(KafkaConsumerType.ERROR, "[invalid type] - ${dto.type}").toJson())
        }
      } // end of when
    } // end of flow
  } // end of accept


  // ________________________________________________________________________________
  // COMMANDS
  // ________________________________________________________________________________
  private suspend fun sampling(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      val debounce = dto.body.toLong()
      if (!isSampling.get() && debounce > 50) {
        emitAll(startSampling(debounce).map { KafkaConsumerDTO(dto.type, it) })
      } else {
        Log.info().log("[sampling already active] - debounce={}", debounce)
        emit(KafkaConsumerDTO(dto.type, "NOK"))
      }
    }.flowOn(context)
  }

  private fun startSampling(debounce: Long) : Flow<String> {
    Log.info().log("[KafkaAction sampling] - starting consumer - {}", debounce)
    messagesReceived.set(0L)
    isSampling.set(true)
    val flow = KafkaConsumerBare.connect(aKafkaConfig, isSampling)
    return flow.sample(debounce).map {
      Log.info().log("[luis] - received message")
      messagesReceived.getAndIncrement()
      return@map mapper.readValue(it.value, MonitorMessage::class.java)
    }.map {
      return@map mapper.writeValueAsString(it)
    }
  }

  private suspend fun configRead(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      val replyDto = KafkaConsumerDTO(KafkaConsumerType.CONFIG_READ, returnConfig().toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun configWrite(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      val config = KafkaConsumerConfig.fromJson(dto.body)
      aKafkaConfig = config.kafkaConfig
      isSampling.set(false)
      samplingPeriod.set(config.samplingPeriod)
      val replyDto = KafkaConsumerDTO(KafkaConsumerType.CONFIG_WRITE, returnConfig().toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  // ________________________________________________________________________________
  // Helper Methods
  // ________________________________________________________________________________
  private fun returnConfig() : KafkaConsumerConfig{
    return KafkaConsumerConfig(
      isSampling = isSampling.get(),
      messagesReceived = messagesReceived.get(),
      samplingPeriod = samplingPeriod.get(),
      kafkaConfig = aKafkaConfig
    )
  }

} // EOF


data class KafkaConsumerDTO(
        var type: KafkaConsumerType = KafkaConsumerType.CONFIG_READ,
        var body: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaConsumerDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

enum class KafkaConsumerType(val id: Int) {
  SAMPLE(1),
  CONFIG_READ(6),
  CONFIG_WRITE(7),
  ERROR(6)
}

data class KafkaConsumerConfig(
  var isSampling: Boolean = false,
  var messagesReceived: Long = 0L,
  var samplingPeriod: Long = 1000L,
  var kafkaConfig: KafkaConfig = KafkaConfig()
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaConsumerConfig>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}


