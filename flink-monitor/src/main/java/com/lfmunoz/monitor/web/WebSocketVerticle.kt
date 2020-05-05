package com.lfmunoz.flink.web

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpConnection
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.fissore.slf4j.FluentLoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Verticle - WebSocket
 */
class WebSocketVerticle(
  private val port: Int,
  private val wsConnectionFactory: (Vertx, ServerWebSocket) -> Handler<String>
) : CoroutineVerticle() {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(WebSocketVerticle::class.java)
  }

  // FIELDS
  private val connectionMap: ConcurrentHashMap<Int, ServerWebSocket> = ConcurrentHashMap()
  private lateinit var server: HttpServer

  // ________________________________________________________________________________
  // VERTICLE START
  // ________________________________________________________________________________
  override suspend fun start() {
    // Initialize WebSocket Server
    val options = HttpServerOptions().setPort(port)
    server = vertx.createHttpServer(options)
    server.webSocketHandler(::webSocketHandler)
    server.exceptionHandler { e -> Log.error().log("server exception: {}", e.message) }
    server.connectionHandler(::connectionHandler)

    try {
      server.listenAwait()
      Log.info().log("[WS LISTENING] - port={} ", server.actualPort())
    } catch(e: Throwable) {
      Log.error().withCause(e).log("Failing to start the ws server ")
    }
  }

  // ________________________________________________________________________________
  // VERTICLE STOP
  // ________________________________________________________________________________
  override suspend fun stop() {
    Log.info().log("Stopping...")
    connectionMap.clear()
    try {
      server.closeAwait()
      Log.info().log("WebSocket server stopped successfully")
    } catch(e: Throwable) {
      Log.error().withCause(e).log("Error stopping WebSocket server")
    }
  }

  // ________________________________________________________________________________
  // METHODS
  // ________________________________________________________________________________
  private fun webSocketHandler(socket: ServerWebSocket) {
    Log.info().log("ws connection established from: {}", socket.remoteAddress().port())
    socket.binaryMessageHandler {
      Log.error().log("Received binary data from {}", socket.remoteAddress().host())
    }
    socket.textMessageHandler(wsConnectionFactory(vertx, socket))
    socket.exceptionHandler { e -> Log.error().log("Socket exception: {}", e.message) }
    socket.closeHandler { Log.info().log("Socket closed") }
    connectionMap[socket.remoteAddress().port()] = socket
  }

  private fun connectionHandler(aHttpConnection: HttpConnection ) {
    val address = aHttpConnection.remoteAddress().host()
    Log.info().log("[{}] - new connection", address)
  }

  fun getConnection(port: Int): ServerWebSocket? {
    return connectionMap[port]
  }


} // EOF

