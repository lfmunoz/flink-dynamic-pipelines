package com.lfmunoz.flink.actions.producer

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.flink.kafka.KafkaConfig
import com.lfmunoz.flink.kafka.KafkaConsumerBare
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.KafkaPublisherBare
import com.lfmunoz.flink.mapper
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class KafkaProducerAction : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaProducerAction::class.java)
  }

  private val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(100)


  private val aKafkaConfig = KafkaConfig(
    groupId = "consumer-default-${Random.nextInt(11234, 99876)}"
  )

  // PRODUCER
  private val messageRatePerSecondInt: Int = 5


  private val isProducing = AtomicBoolean(false)
  private val isSampling= AtomicBoolean(false)
  private val messagesReceived = AtomicLong(0L)
  private val messagesSent = AtomicLong(0L)

  private val job = SupervisorJob()
  private val context = newFixedThreadPoolContext(3, "kafkaAction")
  private val scope = CoroutineScope(context + job)

  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = KafkaActionDTO.fromJson(wsPacket.payload)
      Log.info().log("[KafkaAction] - {}", dto)

      when (dto.type) {
        KafkaActionType.START -> {
          if (isProducing.get()) {
            emit(buildErrorResponse("Already producing"))
          } else {
            messagesSent.set(0L)
            isProducing.set(true)
            emit(buildReply(dto, "OK"))
            scope.launch {
              start()
            }
          }
        }
        KafkaActionType.STOP -> {
          if (isProducing.get()) {
            isProducing.set(false)
            emit(buildReply(dto, "OK"))
          } else {
            emit(buildErrorResponse("Not producing"))
          }
        }
        KafkaActionType.SAMPLE -> {
          if (isSampling.get()) {
            emit(buildErrorResponse("Already sampling"))
          } else {
            messagesReceived.set(0L)
            isSampling.set(true)
            emitAll( sampling().flowOn(context))
          }
        }
        KafkaActionType.CANCEL -> {
          if (isSampling.get()) {
            isSampling.set(false)
            emit(buildReply(dto, "OK"))
          } else {
            emit(buildErrorResponse("Not sampling"))
          }
        }
        KafkaActionType.STATUS -> {
          emit(buildReply(dto, "OK"))
        }
        else -> {
          emit(buildErrorResponse("invalid kafka action type"))
        }
      } // end of when
    } // end of flow
  } // end of accept

  private suspend fun start() {
    val publishDelay = rateToDelayInMillis(messageRatePerSecondInt)
    Log.info().log("[KafkaAction start] - starting publishing rate={}", messageRatePerSecondInt)
    KafkaPublisherBare.connect(aKafkaConfig, flow {
      while (isProducing.get()) {
        val aMonitorMessage = aMonitorMessageDataGenerator.random(20)
        val key = aMonitorMessage.id.toByteArray()
        val value = mapper.writeValueAsBytes(aMonitorMessage)
        messagesSent.getAndIncrement()
        delay(publishDelay)
        emit(KafkaMessage(key, value))
      }
    })
    Log.info().log("[KafkaAction start] - ending publishing")
  }

  private fun sampling() : Flow<String> {
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

  // ________________________________________________________________________________
// Helper Methods
// ________________________________________________________________________________
  private fun buildReply(aKafkaActionDTO: KafkaActionDTO, message: String): String {
    val status = KafkaActionStatus(
      isProducing = isProducing.get(),
      isSampling = isSampling.get(),
      messagesReceived = messagesReceived.get(),
      messagesSent = messagesSent.get(),
      message = message
    ).toJson()
    return KafkaActionDTO(
      type = aKafkaActionDTO.type,
      body = status
    ).toJson()
  }

  private fun buildErrorResponse(text: String): String {
    val status = KafkaActionStatus(
      isProducing = isProducing.get(),
      isSampling = isSampling.get(),
      messagesReceived = messagesReceived.get(),
      messagesSent = messagesSent.get(),
      message = text
    ).toJson()
    return KafkaActionDTO(
      type = KafkaActionType.ERROR,
      body = status
    ).toJson()
  }

  private fun rateToDelayInMillis(rate: Int) : Long {
    return (1000/rate).toLong()

  }

} // EOF


data class KafkaActionDTO(
  var type: KafkaActionType = KafkaActionType.STATUS,
  var body: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaActionDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

enum class KafkaActionType(val id: Int) {
  START(1),
  STOP(2),
  SAMPLE(3),
  CANCEL(4),
  STATUS(5),
  ERROR(6)
}

data class KafkaActionStatus(
  var isProducing: Boolean = false,
  var isSampling: Boolean = false,
  var messagesReceived: Long = 0L,
  var messagesSent: Long = 0L,
  var message: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaActionStatus>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}


