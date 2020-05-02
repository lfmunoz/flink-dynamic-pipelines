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
  private var aKafkaConfig = KafkaConfig()
  private val isSampling= AtomicBoolean(false)
  private val messagesReceived = AtomicLong(0L)

  private val job = SupervisorJob()
  private val context = newSingleThreadContext( "kafkaConsumer")
  private val scope = CoroutineScope(context + job)


  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = KafkaConsumerDTO.fromJson(wsPacket.payload)
      Log.info().log("[KafkaAction] - {}", dto)

      when (dto.type) {
        KafkaConsumerType.SAMPLE -> {
          emitAll(sampling(dto).map{ mapper.writeValueAsString(it)})
        }
        KafkaConsumerType.CANCEL -> {
          emitAll(cancel(dto).map{ mapper.writeValueAsString(it)})
        }
        KafkaProducerType.CONFIG_READ -> {
          emitAll(configRead(dto).map { it.toJson() })
        }
        KafkaProducerType.CONFIG_WRITE -> {
          emitAll(configWrite(dto).map { it.toJson() })
        }
        KafkaConsumerType.STATUS -> {
          emitAll(status(dto).map {  it.toJson() } )
        }
        else -> {
          emit(KafkaProducerDTO(KafkaProducerType.ERROR, "invalid action").toJson())
        }
      } // end of when
    } // end of flow
  } // end of accept


  // ________________________________________________________________________________
  // COMMANDS
  // ________________________________________________________________________________
  private suspend fun sampling(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      if (!isSampling.get()) {
        messagesReceived.set(0L)
        isSampling.set(true)
        scope.launch { startSampling() }
      }
      emit(KafkaConsumerDTO(dto.type, "OK"))
    }.flowOn(context)
  }

  private fun startSampling() : Flow<String> {
    Log.info().log("[KafkaAction sampling] - starting consumer")
    val flow = KafkaConsumerBare.connect(aKafkaConfig, isSampling)
    return flow.debounce(1_00L).map {
      Log.info().log("[luis] - received message")
      messagesReceived.getAndIncrement()
      return@map mapper.readValue(it.value, MonitorMessage::class.java)
    }.map {
      return@map mapper.writeValueAsString(it)
    }
  }

  private suspend fun cancel(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      if (isSampling.get()) {
        isSampling.set(false)
      }
      emit(KafkaConsumerDTO(dto.type, "OK"))
    }.flowOn(context)
  }

  private suspend fun configRead(dto: KafkaConsumerDTO): Flow<KafkaProducerDTO> {
    return flow {
      val replyDto = KafkaProducerDTO(KafkaProducerType.CONFIG_READ, aKafkaConfig.toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun configWrite(dto: KafkaConsumerDTO): Flow<KafkaProducerDTO> {
    return flow {
      val newKafkaConfig = KafkaConfig.fromJson(dto.body)
      aKafkaConfig = newKafkaConfig
      val replyDto = KafkaProducerDTO(KafkaProducerType.CONFIG_WRITE, aKafkaConfig.toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun status(dto: KafkaConsumerDTO): Flow<KafkaConsumerDTO> {
    return flow {
      val status = KafkaConsumerStatus(isSampling = false, messagesReceived = messagesReceived.get())
      val replyDto = KafkaConsumerDTO(KafkaConsumerType.STATUS, status.toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  // ________________________________________________________________________________
  // Helper Methods
  // ________________________________________________________________________________


} // EOF


data class KafkaConsumerDTO(
        var type: KafkaConsumerType = KafkaConsumerType.STATUS,
        var body: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaConsumerDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

enum class KafkaConsumerType(val id: Int) {
  SAMPLE(1),
  CANCEL(2),
  CONFIG_READ(6),
  CONFIG_WRITE(7),
  STATUS(5),
  ERROR(6)
}

data class KafkaConsumerStatus(
  var isSampling: Boolean = false,
  var messagesReceived: Long = 0L
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaConsumerStatus>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}


