package com.lfmunoz.monitor.actions.kafkaAdmin

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import com.lfmunoz.monitor.actions.producer.MonitorMessage
import com.lfmunoz.monitor.actions.producer.MonitorMessageDataGenerator
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.kafka.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class KafkaAdminAction : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaAdminAction::class.java)
  }

  private val bash = BashService()
  private val aKafkaAdminBare = KafkaAdminBash(bash)

  private val job = SupervisorJob()
  private val context = newSingleThreadContext( "kafkaAction")
  private val scope = CoroutineScope(context + job)

  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = KafkaAdminDTO.fromJson(wsPacket.payload)
      Log.info().log("[KafkaAdminAction] - {}", dto)

      when (dto.type) {
        KafkaAdminActionType.TOPICS -> {
            emitAll(topics(dto).map{ mapper.writeValueAsString(it)})
        }
        KafkaAdminActionType.CONSUMERS -> {
          emitAll(consumers(dto).map{ mapper.writeValueAsString(it)})
        }
      } // end of when
    } // end of flow
  } // end of accept


  private suspend fun topics(dto: KafkaAdminDTO) : Flow<KafkaAdminDTO> {
    return flow {
      val list = aKafkaAdminBare.listTopics()
      val replyDto = KafkaAdminDTO(dto.type, mapper.writeValueAsString(list))
      emit(replyDto)
    }.flowOn(context)
  }

  private suspend fun consumers(dto: KafkaAdminDTO) : Flow<KafkaAdminDTO> {
    return flow {
      val list = aKafkaAdminBare.listConsumerGroups()
      val replyDto = KafkaAdminDTO(dto.type, mapper.writeValueAsString(list))
      emit(replyDto)
    }.flowOn(context)
  }



  // ________________________________________________________________________________
// Helper Methods
// ________________________________________________________________________________



} // EOF

// ________________________________________________________________________________
// DTO
// ________________________________________________________________________________
data class KafkaAdminDTO(
  var type: KafkaAdminActionType = KafkaAdminActionType.TOPICS,
  var body: String = ""
) {
  companion object {
    fun fromJson(json: String) = mapper.readValue<KafkaAdminDTO>(json)
  }

  fun toJson(): String = mapper.writeValueAsString(this)
}

enum class KafkaAdminActionType(val id: Int) {
  TOPICS(1),
  CONSUMERS(2)
}
