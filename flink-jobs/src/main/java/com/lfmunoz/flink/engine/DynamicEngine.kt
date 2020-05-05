package com.lfmunoz.flink.engine

import com.lfmunoz.flink.monitor.MonitorMessage
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe
import javax.script.ScriptEngineManager

/**
 * Singleton - Dynamic Engine
 */
@ThreadSafe
object DynamicEngine {

  private val engineKts = ScriptEngineManager().getEngineByExtension("kts")

  // These imports will be inserted to all scripts because they are required
  private val scriptPrefix = """
   import com.lfmunoz.flink.monitor.MonitorMessage
   import com.lfmunoz.flink.monitor.MapperResult
  """.trimIndent()

  private val mapperFunctions: ConcurrentHashMap<String, MapperObj> = ConcurrentHashMap()

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
      val lambdaFunc  = engineKts.eval(preEval(script)) as MonitorLambda
      mapperFunctions[key] = MapperObj(script, lambdaFunc)
    } catch(e: RuntimeException) {
      println("could not compile key=$key script=$script")
      e.printStackTrace()
    }
  }

  fun remove(key: String) {
    mapperFunctions.remove(key)
  }

  fun evaluate(monitorMessage: MonitorMessage) : MapperResult {
    val start =System.currentTimeMillis()
    return  mapperFunctions.entries.map {
      it.value.lambda.invoke(monitorMessage)
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
    return mapperFunctions.entries.associate { it.key to it.value.script }
  }

  //________________________________________________________________________________
  // PRIVATE METHODS
  //________________________________________________________________________________
  private fun preEval(script: String) : String { return scriptPrefix + script }

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
  var values: MutableMap<String, String> = mutableMapOf()
) : Serializable {
  constructor(m: MonitorMessage): this(m.id, m.informTime, m.version) {
    startTime = System.currentTimeMillis()
  }
}


