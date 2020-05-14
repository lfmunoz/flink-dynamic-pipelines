package com.lfmunoz.flink.engine

import com.lfmunoz.flink.monitor.MonitorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.util.Collector
import org.fissore.slf4j.FluentLoggerFactory
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe
import javax.script.ScriptEngineManager

//________________________________________________________________________________
// FLINK MAPPER PROCCESS FUNCTION
//________________________________________________________________________________
object DynamicMapperProcessFunction: BroadcastProcessFunction<MonitorMessage, Map<String, String>, MapperResult>() {

  private val Log = FluentLoggerFactory.getLogger(DynamicMapperProcessFunction::class.java)

  override fun processElement(value: MonitorMessage, ctx: ReadOnlyContext, out: Collector<MapperResult>) {
    Log.info().log("[got element] - $value")
    out.collect(DynamicEngine.evaluate(value))
  }

  override fun processBroadcastElement(value: Map<String, String>, ctx: Context, out: Collector<MapperResult>) {
    Log.info().log("[got config] - $value")
    DynamicEngine.update(value)
  }

}

//________________________________________________________________________________
// SINGLETON - DYNAMIC ENGINE
//________________________________________________________________________________
@ThreadSafe
object DynamicEngine {

  private val Log = FluentLoggerFactory.getLogger(DynamicEngine::class.java)

  private val engineKts = ScriptEngineManager().getEngineByExtension("kts")

  private val mapperFunctions: ConcurrentHashMap<String, ScriptCtx> = ConcurrentHashMap()

  init {
    // This will preload the script engine, we don't want it lazy loaded
    engineKts.eval("assert(true)")
  }

  //________________________________________________________________________________
  // PUBLIC METHODS
  //________________________________________________________________________________
  fun update(newMap: Map<String, String>) {
    val newKeys = newMap.minus(mapperFunctions.keys)
    newKeys.forEach { insert(it.key, it.value) }
    val removedKeys = mapperFunctions.minus(newMap.keys)
    removedKeys.forEach { remove(it.key) }
  }

  @Synchronized fun insert(key: String, script: String) {
    try {
      // calling eval is not thread safe, should rarely be called
      engineKts.put("key", key)
      engineKts.put("script", script)
      val aScriptCtx = engineKts.eval(generateKts(key, script)) as ScriptCtx
      mapperFunctions[key] = aScriptCtx
    } catch(e: RuntimeException) {
      Log.warn().withCause(e).log("could not compile key=$key script=$script")
      e.printStackTrace()
    }
  }

  fun remove(key: String) {
    mapperFunctions.remove(key)
  }

  fun evaluate(monitorMessage: MonitorMessage) : MapperResult {
    val start =System.currentTimeMillis()
    return  mapperFunctions.entries.map {
      try {
        it.value.main(monitorMessage)
      } catch(e: RuntimeException) {
        Log.warn().withCause(e).log("error running key=${it.key}")
        MapperResult()
      }
    }.fold(MapperResult()) { acc: MapperResult, item: MapperResult ->
      acc.values.putAll(item.values)
      acc
    }.apply {
      id = monitorMessage.id
      informTime = monitorMessage.informTime
      startTime = start
      version = monitorMessage.version
      computeDelta = System.currentTimeMillis() - start
    }
  }

  fun mapOfLambdas() : Map<String, String> {
    return mapperFunctions.entries.associate { it.key to it.value.script}
  }

  //________________________________________________________________________________
  // PRIVATE METHODS
  //________________________________________________________________________________


}

//________________________________________________________________________________
// DEFINITIONS
//________________________________________________________________________________
typealias MonitorLambda = (MonitorMessage) -> MapperResult

data class MapperDto(
  var key: String = "",
  var script: String = ""
)

data class MapperObj(
  val script: String,
  val lambda: MonitorLambda
)

data class MapperResult(
  var id: String = "N/A",
  var informTime: Long = 0,
  var version: Int = 0,
  var startTime: Long = 0,
  var computeDelta: Long =  0,
  var values: MutableMap<String, Any?> = mutableMapOf()
) : Serializable {
  constructor(m: MonitorMessage): this(m.id, m.informTime, m.version) {
    startTime = System.currentTimeMillis()
  }
}


