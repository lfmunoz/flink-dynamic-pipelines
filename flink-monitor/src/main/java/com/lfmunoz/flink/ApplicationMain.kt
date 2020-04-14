package com.lfmunoz.flink

import com.lfmunoz.flink.actions.producer.KafkaProducerAction
import com.lfmunoz.flink.actions.test.TestAction
import com.lfmunoz.flink.web.*
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.ServerWebSocket
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger


val myModule = module(createdAtStart = true) {
  // VERTX
  single { VertxOptions() }
  single { Vertx.vertx(get()) }
  // ACTIONS
  single(named("TestAction")) { TestAction() as ActionInterface }
  single(named("KafkaAction")) { KafkaProducerAction() as ActionInterface }
  single(named("ActionsMap")) {
    hashMapOf<Int, ActionInterface>(
      WsPacketType.TEST.id to get(named("TestAction")),
      WsPacketType.KAFKA.id to get(named("KafkaAction"))
    )
  }
}

fun main() {
  val koinApp = startKoin {
    SLF4JLogger(Level.DEBUG)
    modules(myModule)
  }

  val vertx = koinApp.koin.get<Vertx>()
  val wsPort = koinApp.koin.getProperty<Int>("PORT", 1991)
  val aAppConfig = AppConfig()
  println(aAppConfig)
  startWs(vertx, aAppConfig)
  startHttp(vertx, aAppConfig)
}

private fun startWs(vertx: Vertx, aAppConfig: AppConfig) {
  val aWebSocketVerticle = WebSocketVerticle(aAppConfig.wsPort, ::connectionFactory)
  vertx.deployVerticle(aWebSocketVerticle) {
    if (it.succeeded()) {
      println("WS deployment id=${it.result()}")
    } else {
      println("Deployment failed! - ${it.cause()}")
    }
  }
}

private fun startHttp(vertx: Vertx, aAppConfig: AppConfig) {
  val aHttpVerticle = HttpVerticle(aAppConfig)
  vertx.deployVerticle(aHttpVerticle) {
    if (it.succeeded()) {
      println("HTTP deployment id=${it.result()}")
    } else {
      println("HTTP deployment failed! - ${it.cause()}")
    }
  }
}

private fun connectionFactory(vertx: Vertx, socket: ServerWebSocket): Handler<String> {
  return OurWSConnection(vertx, socket)
}

