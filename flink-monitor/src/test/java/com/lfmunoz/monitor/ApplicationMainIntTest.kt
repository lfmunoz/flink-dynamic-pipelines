package com.lfmunoz.monitor


import com.lfmunoz.flink.web.AppConfig
import com.lfmunoz.flink.web.WebSocketVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.webSocketAwait
import io.vertx.kotlin.core.undeployAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 * Integration Test - ApplicationMain
 */
@ExtendWith(VertxExtension::class)
class ApplicationMainIntTest {

  private val url = "/"
  private val host = "localhost"
  private val totalConnections = 100

  private var wsPort = ThreadLocalRandom.current().nextInt(4444, 6666)

  // ________________________________________________________________________________
  // Test Cases
  // ________________________________________________________________________________
  @Test
  fun `we can establish multiple ws connections`(vertx: Vertx, testContext: VertxTestContext) {
    val countDownLatch = CountDownLatch(totalConnections)
    val aAppConfig = AppConfig(wsPort=wsPort)
    val aWebSocketVerticle = WebSocketVerticle(aAppConfig.wsPort) connectionFactory@ { _, _ ->
      return@connectionFactory Handler<String> { countDownLatch.countDown() }
    }
    val id = runBlocking(vertx.dispatcher()) { vertx.deployVerticleAwait(aWebSocketVerticle) }
    runBlocking(vertx.dispatcher()) {
      repeat(totalConnections) {
        launch {
          val options = HttpClientOptions().setConnectTimeout(1000)
          val wsClient = vertx.createHttpClient(options)
          val socket  = wsClient.webSocketAwait(aAppConfig.wsPort, host, url)
          socket.writeTextMessage("ping")
          val clientPort = socket.localAddress().port()
          assertThat(aWebSocketVerticle.getConnection(clientPort)).isNotNull()
          socket.close()
        }
      }
    }
    assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue()
    runBlocking(vertx.dispatcher()) { vertx.undeployAwait(id) }
    testContext.completeNow()
  }


} // EOF
