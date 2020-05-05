package com.lfmunoz.monitor.actions.kafkaAdmin

import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.monitor.mapper
import com.lfmunoz.monitor.actions.producer.MonitorMessage
import com.lfmunoz.monitor.actions.producer.MonitorMessageDataGenerator
import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.CmdResult
import com.lfmunoz.monitor.kafka.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory

class KafkaAdminAction(
  private val aKafkaAdminBare: KafkaAdminBash
) : ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(KafkaAdminAction::class.java)
  }

  private val job = SupervisorJob()
  private val context = newSingleThreadContext("kafkaAction")
  private val scope = CoroutineScope(context + job)

  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> = flow {
    val dto = KafkaAdminDTO.fromJson(wsPacket.payload)
    Log.info().log("[KafkaAdminAction] - {}", dto)
    when (dto.type) {
      KafkaAdminActionType.TOPICS -> emitAll(topics(dto).map { it.toJson() })
      KafkaAdminActionType.CONSUMERS -> emitAll(consumers(dto).map { it.toJson() })
    }
  }

  // ________________________________________________________________________________
  // PRIVATE
  // ________________________________________________________________________________
  private suspend fun topics(dto: KafkaAdminDTO): Flow<KafkaAdminDTO> = flow {
    aKafkaAdminBare.listTopics().forEach {
      when (it) {
        is CmdResult.Stdout ->  {
          dtoEmitter(it, ::emit)
          aKafkaAdminBare.describeTopic(it.line).forEach {dtoEmitter(it, ::emit) }
          aKafkaAdminBare.diskUsage(it.line).forEach {dtoEmitter(it, ::emit) }
        }
        is CmdResult.Success -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_SUCCESS, ""))
        else -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_FAILURE, "$it"))
      }
    }
  }.flowOn(context)

  private suspend fun consumers(dto: KafkaAdminDTO): Flow<KafkaAdminDTO> = flow {
    aKafkaAdminBare.listConsumerGroups().forEach {
      when (it) {
        is CmdResult.Stdout ->  {
          emit(KafkaAdminDTO(KafkaAdminActionType.RESP_STDOUT, it.line))
          aKafkaAdminBare.describeConsumerGroup(it.line).forEach {dtoEmitter(it, ::emit) }
        }
        is CmdResult.Success -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_SUCCESS, ""))
        else -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_FAILURE, "$it"))
      }
    }
  }.flowOn(context)

  private suspend fun dtoEmitter(aCmdResult: CmdResult, emit: suspend (KafkaAdminDTO) -> Unit) {
    when (aCmdResult) {
      is CmdResult.Stdout ->   emit(KafkaAdminDTO(KafkaAdminActionType.RESP_STDOUT, aCmdResult.line))
      is CmdResult.Success -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_SUCCESS, ""))
      else -> emit(KafkaAdminDTO(KafkaAdminActionType.RESP_FAILURE, "$aCmdResult"))
    }
  }

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
  CONSUMERS(2),

  RESP_STDOUT(3),
  RESP_SUCCESS(4),
  RESP_FAILURE(5)

}
