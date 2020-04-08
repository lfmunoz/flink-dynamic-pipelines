package com.lfmunoz.flink


import com.lfmunoz.flink.web.WebSocketVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.webSocketAwait
import io.vertx.kotlin.core.undeployAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger


/**
 * Integration Test - ApplicationMain
 */
@ExtendWith(VertxExtension::class)
class ApplicationMainIntTest {

  companion object {
    private const val URL = "/"
    private const val HOST = "localhost"
    private const val TOTAL_CONNECTIONS = 100
  }

  // Dependencies
  private var port: Int = 0
  private var id  = "N/A"
  private lateinit var atomicCounter: AtomicInteger

  // Unit Under Test
  private lateinit var webSocketVerticle: WebSocketVerticle

  // ________________________________________________________________________________
  // SetUp / TearDown
  // ________________________________________________________________________________
  @BeforeEach
  fun `before each`(vertx: Vertx, testContext: VertxTestContext) {
    atomicCounter = AtomicInteger(0)
    port = ThreadLocalRandom.current().nextInt(4444, 6666)
    webSocketVerticle = WebSocketVerticle(port, ::connectionFactory)

    runBlocking(vertx.dispatcher()) {
      id = vertx.deployVerticleAwait(webSocketVerticle)
    }
    testContext.completeNow()
  }

  @AfterEach
  fun `after each`(vertx: Vertx, testContext: VertxTestContext) {
    runBlocking(vertx.dispatcher()) {
      vertx.undeployAwait(id)
    }
    testContext.completeNow()
  }

  // ________________________________________________________________________________
  // Test Cases
  // ________________________________________________________________________________
  @Test
  fun `We can establish multiple WebSocket connections`(vertx: Vertx, testContext: VertxTestContext) {
    runBlocking(vertx.dispatcher()) {
      repeat(TOTAL_CONNECTIONS) {
        launch {
          val options = HttpClientOptions().setConnectTimeout(1000)
          val wsClient = vertx.createHttpClient(options)
          val socket  = wsClient.webSocketAwait(port, HOST, URL)
          socket.writeTextMessage("ping")
          delay(100)
          val clientPort = socket.localAddress().port()
          assertThat(webSocketVerticle.getConnection(clientPort)).isNotNull()
          socket.close()
        }
      }
    }
    assertThat(atomicCounter.get()).isEqualTo(TOTAL_CONNECTIONS)
    testContext.completeNow()
  }

  // ________________________________________________________________________________
  // helper methods
  // ________________________________________________________________________________
  private fun connectionFactory(vertx: Vertx, socket: ServerWebSocket) : Handler<String> {
    return Handler<String> {
      atomicCounter.incrementAndGet()
    }
  }

} // EOF
