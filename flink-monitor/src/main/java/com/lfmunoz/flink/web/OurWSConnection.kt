package com.lfmunoz.flink.web

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import com.lfmunoz.flink.mapper
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.fissore.slf4j.FluentLoggerFactory
import org.koin.core.KoinComponent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.utils.ThreadSafe
import org.koin.core.get
import org.koin.core.qualifier.named
import java.lang.IllegalArgumentException
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * OurWsConnection
 *  Handle string objects coming from client (should all be JSON String of type WsPacket)
 *
 *  ThreadSafe by Vertx context
 */
class OurWSConnection(
  private val vertx: Vertx,
  private val socket: ServerWebSocket
) : Handler<String>, CoroutineScope, KoinComponent {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(OurWSConnection::class.java)
  }

  // FIELDS
  private var isAuthenticated: Boolean = false
//  private val crudAction by inject<CrudAction>()
//  private val testAction by inject<TestAction>()
//  private val taskAction by inject<TaskAction>()
//  private val aCrudAction = get<CrudAction>()
//  private val aTestAction = get<TestAction>()
//  private val aTaskAction = get<TaskAction>()
//
//  private val actionHolder: Map<Int, ActionInterface> = hashMapOf(
//    WsPacketType.KAFKA to ,
//    WsPacketType.TEST.id to aTestAction,
//    WsPacketType.TASK.id to aTaskAction
//  )
  private val actionHolder: HashMap<Int, ActionInterface> = get(named("ActionsMap"))

  private val job = Job()
  override val coroutineContext: CoroutineContext
    get() = vertx.dispatcher() + job

  // ________________________________________________________________________________
  // handle()
  // ________________________________________________________________________________
  override fun handle(text: String) {
    try {
      val aWsPacket = WsPacket.fromJson(text)
      if (isAuthenticated) processAuthorized(aWsPacket) else processNotAuthenticated(aWsPacket)
    } catch (e: JsonProcessingException) {
      Log.warn().withCause(e).log("Invalid JSON packet received {}", text)
      socket.close()
    }
  }

  // ________________________________________________________________________________
  // NOT AUTHENTICATED
  // ________________________________________________________________________________
  private fun processNotAuthenticated(packet: WsPacket) {
    when (packet.action) {
      WsPacketType.AUTH -> {
        // TODO: Check username / password. Right now we just authenticate with any AUTH Packet
        Log.info().log("Client authenticated successfully")
        isAuthenticated = true
        val response = buildWsPacketAuthLack(packet)
        socket.writeTextMessage(response)
      }
      else -> {
        Log.info().log("Unexpected packet before Authentication")
        socket.close()
      }
    }
  }

  // ________________________________________________________________________________
  // AUTHENTICATED
  //
  //  TODO: For invalid packets we can either log and ignore or throw an expcetion.
  //    I think we should throw error and disconnect because invalid packets
  //    should never happen. Unlikely event.
  // ________________________________________________________________________________
  private fun processAuthorized(aWsPacket: WsPacket) {
    Log.info().log("Processing aWsPacket - {}", aWsPacket)
    val response = returnActionFlow(aWsPacket)
    launch { runAction(response, aWsPacket) }
  }

  private suspend fun runAction(response: Flow<String>, aWsPacket: WsPacket) {
    val ackFlow = response.map {
      buildWsPacketResponse(aWsPacket, it)
    }
    flowOf(fackFlow(aWsPacket), ackFlow).flattenConcat().catch {
      Log.debug().withCause(it).log("Action Error ");
      emit(buildWsPacketError(aWsPacket, it.toString()))
    }.onCompletion {
      emit(buildWsPacketLack(aWsPacket))
    }.collect { text -> socket.writeTextMessage(text) }
  }

  private fun returnActionFlow(aWsPacket: WsPacket): Flow<String> {
    return actionHolder[aWsPacket.action.id]?.let {
      it.accept(aWsPacket)
    }  ?: flow {
      throw IllegalArgumentException("Invalid packet, it has an unknown action - ${aWsPacket.action}")
    }
  }

  private fun fackFlow(reqWsPacket: WsPacket) : Flow<String> {
    return flow {
      emit(buildWsPacketFack(reqWsPacket))
    }
  }

} // EOF


