package com.lfmunoz.flink.web


import com.lfmunoz.flink.readTextFile
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.fissore.slf4j.FluentLoggerFactory
import java.io.File

/**
 *
 */
class HttpVerticle(private val aAppConfig: AppConfig) : CoroutineVerticle() {
  companion object {
    private val LOG = FluentLoggerFactory.getLogger(HttpVerticle::class.java)
  }

  lateinit var eb : EventBus
  private val indexHtml = readTextFile("index.html")

  override suspend fun start() {
    LOG.info().log("[HTTP LISTENING] - port={}", aAppConfig.httpPort)
    eb = vertx.eventBus()
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.get("/").handler (rootEndpoint())
    vertx.createHttpServer().requestHandler(router).listen(aAppConfig.httpPort)
  }

  // ________________________________________________________________________________
  // Endpoint handlers
  //________________________________________________________________________________
  private fun rootEndpoint()= Handler<RoutingContext> { ctx ->
    ctx.response().end(indexHtml)
  }

  private fun managementOptionsEndpoint()  = Handler<RoutingContext> {ctx ->
    ctx.response().putHeader("Access-Control-Allow-Origin", "*")
    ctx.response().putHeader("Access-Control-Allow-Headers", "*")
      .end()
  }

}


