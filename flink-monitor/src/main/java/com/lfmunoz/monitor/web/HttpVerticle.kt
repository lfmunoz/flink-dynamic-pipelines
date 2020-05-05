package com.lfmunoz.flink.web


import com.lfmunoz.monitor.readTextFile
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.fissore.slf4j.FluentLoggerFactory

/**
 *
 */
class HttpVerticle(private val aAppConfig: AppConfig) : CoroutineVerticle() {
  companion object {
    private val LOG = FluentLoggerFactory.getLogger(HttpVerticle::class.java)
  }

  lateinit var eb : EventBus
  private val indexHtml = readTextFile("<!doctype html>\n\n<html lang=\"en\">\n\n<head>\n  <meta charset=\"utf-8\">\n\n  <title>Flink Pipeline</title>\n  <meta name=\"description\" content=\"flink pipeline\">\n  <meta name=\"author\" content=\"lfm\">\n\n  <style type=\"text/css\"> </style>\n</head>\n\n<body>\n<!-- START OF APP -->\n<div id=\"app\">\n  <div class=\"main\">\n    <h1>Flink Pipeline</h1>\n  </div>\n</div>\n<!-- END OF APP -->\n\n</body>\n\n</html>\n")

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


