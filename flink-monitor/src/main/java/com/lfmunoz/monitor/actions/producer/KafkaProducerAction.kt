package com.lfmunoz.monitor.actions.producer

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.monitor.kafka.*
import com.lfmunoz.monitor.kafka.KafkaConfig
import kotlinx.coroutines.*
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
class KafkaProducerAction : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaProducerAction::class.java)
  }

  // SERVICES
  private val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(100)

  // FIELDS
  private val job = SupervisorJob()
  private val context = newSingleThreadContext("kProd")
  private val scope = CoroutineScope(context + job)

  // PRODUCER
  // @GuardedBy access only from context
  private var aKafkaConfig = KafkaConfig()
  private var isProducing = AtomicBoolean(false)
  private var messagesSent = AtomicLong(0L)
  private var messageRatePerSecondInt = AtomicInteger(1)


  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = KafkaProducerDTO.fromJson(wsPacket.payload)
      Log.info().log("[KafkaProducerAction] - {}", dto)
      when (dto.type) {
        KafkaProducerType.START -> {
          emitAll(produce(dto).map { it.toJson() })
        }
        KafkaProducerType.CONFIG_READ -> {
          emitAll(configRead(dto).map { it.toJson() })
        }
        KafkaProducerType.CONFIG_WRITE -> {
          emitAll(configWrite(dto).map { it.toJson() })
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
  private suspend fun produce(dto: KafkaProducerDTO) : Flow<KafkaProducerDTO> {
    return flow {
      if (!isProducing.get()) {
        messagesSent.set(0L)
        isProducing.set(true)
        emitAll(startProducer().map {
          return@map when(it) {
            is KafkaPublisherResult.Published -> {
              val value = mapper.readValue<MonitorMessage>(it.kafkaMessage.value)
              KafkaProducerDTO(type = KafkaProducerType.START, body = value.toJson())
            }
            else -> {
              KafkaProducerDTO(type = KafkaProducerType.START, body = mapper.writeValueAsString(it))

            }

          }
        })
      }
    }
  }

  private suspend fun startProducer() : Flow<KafkaPublisherResult> {
    val publishDelay = rateToDelayInMillis(messageRatePerSecondInt.get())
    Log.info().log("[KafkaAction start] - starting publishing rate={}", messageRatePerSecondInt)
    return KafkaPublisherBare.connect(aKafkaConfig, flow{
      while (isProducing.get()) {
        val aMonitorMessage = aMonitorMessageDataGenerator.random(20)
        val key = aMonitorMessage.id.toByteArray()
        val value = mapper.writeValueAsBytes(aMonitorMessage)
        messagesSent.getAndIncrement()
        delay(publishDelay)
        emit(KafkaMessage(key, value))
      }
    })

  }

  private suspend fun configRead(dto: KafkaProducerDTO): Flow<KafkaProducerDTO> {
    return flow {
      val replyDto = KafkaProducerDTO(KafkaProducerType.CONFIG_READ, returnConfig().toJson())
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun configWrite(dto: KafkaProducerDTO): Flow<KafkaProducerDTO> {
    return flow {
      val config = KafkaProducerConfig.fromJson(dto.body)
      aKafkaConfig = config.kafkaConfig
      isProducing.set(false)
      messageRatePerSecondInt.set(config.messageRatePerSecondInt)
      val replyDto = KafkaProducerDTO(KafkaProducerType.CONFIG_WRITE, returnConfig().toJson())
      emit(replyDto)
    }.flowOn(context)
  }


  // ________________________________________________________________________________
  // Helper Methods
  // ________________________________________________________________________________
  private fun rateToDelayInMillis(rate: Int): Long {
    return (1000 / rate).toLong()
  }

  private fun returnConfig() : KafkaProducerConfig{
    return KafkaProducerConfig(
      isProducing = isProducing.get(),
      messagesSent = messagesSent.get(),
      messageRatePerSecondInt = messageRatePerSecondInt.get(),
      kafkaConfig = aKafkaConfig
    )
  }

} // EOF


//________________________________________________________________________________
// DTO
//________________________________________________________________________________
data class KafkaProducerDTO(
  var type: KafkaProducerType = KafkaProducerType.CONFIG_READ,
  var body: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaProducerDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

//________________________________________________________________________________
// DTO TYPE
//________________________________________________________________________________
enum class KafkaProducerType(val id: Int) {
  START(1),
  CONFIG_READ(6),
  CONFIG_WRITE(7),
  ERROR(31)
}

//________________________________________________________________________________
// DTO STATUS
//________________________________________________________________________________
data class KafkaProducerConfig(
  var isProducing: Boolean = false,
  var messagesSent: Long = 0L,
  var messageRatePerSecondInt: Int = 1,
  var kafkaConfig: KafkaConfig = KafkaConfig()
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaProducerConfig>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}


