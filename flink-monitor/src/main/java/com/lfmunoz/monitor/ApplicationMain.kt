package com.lfmunoz.monitor

import com.lfmunoz.monitor.actions.producer.KafkaProducerAction
import com.lfmunoz.monitor.actions.test.TestAction
import com.lfmunoz.flink.web.*
import com.lfmunoz.monitor.actions.consumer.KafkaConsumerAction
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.ServerWebSocket
import org.apache.kafka.clients.consumer.ConsumerConfig
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
  single(named("KafkaProducerAction")) { KafkaProducerAction() as ActionInterface }
  single(named("KafkaConsumerAction")) { KafkaConsumerAction() as ActionInterface }
  single(named("KafkaAdminAction")) { KafkaProducerAction() as ActionInterface }
  single(named("ActionsMap")) {
    hashMapOf<Int, ActionInterface>(
      WsPacketType.TEST.id to get(named("TestAction")),
      WsPacketType.KAFKA_PRODUCER.id to get(named("KafkaProducerAction")),
      WsPacketType.KAFKA_CONSUMER.id to get(named("KafkaConsumerAction")),
      WsPacketType.KAFKA_ADMIN.id to get(named("KafkaAdminAction"))
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

