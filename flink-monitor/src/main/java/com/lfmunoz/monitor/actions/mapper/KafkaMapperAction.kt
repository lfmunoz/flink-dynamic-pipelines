package com.lfmunoz.monitor.actions.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.monitor.MapOfStringStringType
import com.lfmunoz.monitor.kafka.*
import com.lfmunoz.monitor.kafka.KafkaConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe
import javax.management.monitor.Monitor
import kotlin.random.Random

@ThreadSafe
class KafkaMapperAction(
  private val aKafkaAdminBare: KafkaAdminBash
) : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaMapperAction::class.java)
  }

  // FIELDS
  private val job = SupervisorJob()
  private val context = newSingleThreadContext("kProd")
  private val scope = CoroutineScope(context + job)

  // PRODUCER
  @get:Synchronized
  @set:Synchronized
  private var aKafkaConfig = KafkaConfig(topic = "mapper-topic")
  private var offset = AtomicInteger(0)
  private var isProducing = AtomicBoolean(false)

  private val channel = Channel<Map<String, String>>(5)

  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> = flow {
    val dto = KafkaMapperDTO.fromJson(wsPacket.payload)
    Log.info().log("[accept] - {}", dto)
    when (dto.type) {
      KafkaMapperType.SEND_MAPPER -> {
        emitAll(sendMapper(dto).map { it.toJson() })
      }
      KafkaMapperType.CONFIG_WRITE -> {
        emitAll(configWrite(dto).map { it.toJson() })
      }
      KafkaMapperType.CONFIG_READ -> {
        emitAll(configRead(dto).map { it.toJson() })
      }
      else -> {
        emit(KafkaMapperDTO(KafkaMapperType.ERROR, "invalid action").toJson())
      }
    }
  }


  // ________________________________________________________________________________
  // COMMANDS
  // ________________________________________________________________________________
  private suspend fun sendMapper(dto: KafkaMapperDTO): Flow<KafkaMapperDTO> = flow {
    if (!isProducing.get()) {
      emit(KafkaMapperDTO(dto.type, "[NOK] - Publisher is not active, write config first"))
      return@flow
    }
    val newConfig = mapper.readValue(dto.body, MapOfStringStringType) as Map<String, String>
    channel.send(newConfig)
    emit(KafkaMapperDTO(dto.type, "OK"))
  }

  private suspend fun startPublisher() : Flow<KafkaPublisherResult>  {
    return KafkaPublisherBare.connect(aKafkaConfig, flow {
      while (isProducing.get()) {
        val mappingConfig = channel.receive()
        val key = mapper.writeValueAsBytes(offset.getAndIncrement())
        val value = mapper.writeValueAsBytes(mappingConfig)
        emit(KafkaMessage(key, value))
      }
    })
  }

  private suspend fun configRead(dto: KafkaMapperDTO): Flow<KafkaMapperDTO> {
    return flow {
      val lastMessage = aKafkaAdminBare.getLastMessage(aKafkaConfig.topic)
      val replyDto = KafkaMapperDTO(KafkaMapperType.CONFIG_READ, returnConfig(lastMessage).toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun configWrite(dto: KafkaMapperDTO): Flow<KafkaMapperDTO> {
    return flow {
      val config = KafkaMapperConfig.fromJson(dto.body)
      aKafkaConfig = config.kafkaConfig
      if(isProducing.get()) {
        isProducing.set(false)
        val replyDto = KafkaMapperDTO(KafkaMapperType.CONFIG_WRITE, returnConfig("STOPPED").toJson())
        emit(replyDto)
      } else {
        scope.launch {
          startPublisher().collect {
            Log.info().log("[mapper publish] - ${it}")
          }
        }
        isProducing.set(true)
        val replyDto = KafkaMapperDTO(KafkaMapperType.CONFIG_WRITE, returnConfig("STARTED").toJson())
        emit(replyDto)
      }
    }.flowOn(context)
  }

  // ________________________________________________________________________________
  // Helper Methods
  // ________________________________________________________________________________
  private fun returnConfig(lastMessage: String): KafkaMapperConfig {
    return KafkaMapperConfig(
      offset = offset.get(),
      isProducing = isProducing.get(),
      lastMessage = lastMessage,
      kafkaConfig = aKafkaConfig
    )
  }

} // EOF


//________________________________________________________________________________
// DTO
//________________________________________________________________________________
data class KafkaMapperDTO(
  var type: KafkaMapperType = KafkaMapperType.CONFIG_READ,
  var body: String = "",
  var version: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaMapperDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

//________________________________________________________________________________
// DTO TYPE
//________________________________________________________________________________
enum class KafkaMapperType(val id: Int) {
  SEND_MAPPER(1),
  CONFIG_READ(6),
  CONFIG_WRITE(7),
  ERROR(31)
}

//________________________________________________________________________________
// DTO STATUS
//________________________________________________________________________________
data class KafkaMapperConfig(
  var offset: Int = 0,
  var isProducing : Boolean = false,
  var lastMessage: String = "",
  var kafkaConfig: KafkaConfig = KafkaConfig()
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaMapperConfig>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}


