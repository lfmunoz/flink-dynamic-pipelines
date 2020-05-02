package com.lfmunoz.flink

import com.lfmunoz.flink.flink.FlinkJobContext
import com.lfmunoz.flink.flink.bufferToByteArray
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSource
import com.lfmunoz.flink.rabbit.rabbitSink
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import java.util.concurrent.ConcurrentHashMap
import javax.script.ScriptEngineManager

// Flink Job:  Kafka --> Rabbit Bridge
//fun dynamicMapperJob(jobCtx: FlinkJobContext) {
//  jobCtx




//  jobCtx.env.execute("[Monitor Message] Kafka to Rabbit Bridge")
//}
//

typealias IntToInt = (Int) -> Int
typealias DoubleToDouble = (Double) -> Double


fun main() {
  println("test")


//  val engineKts = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine
//  val comp1 = engineKts.compile("val x = 3")
  val mapperObjList = mutableListOf<MapperObj>()
  mapperObjList.add( MapperObj(1L, "increment", "{ x: Int -> x + 1 }") )
  mapperObjList.add( MapperObj(2L, "double", "{ x: Int -> x * 2 }") )
  mapperObjList.add( MapperObj(3L, "square", "{ x: Int -> x * x }") )
  mapperObjList.add( MapperObj(4L, "decrement", "{ x: Int -> x - 1 }") )

  val start = System.currentTimeMillis()
  val mapperConfig = MapperConfig()
  val finish = System.currentTimeMillis()
  println(" Application has started [${finish-start }ms]")

  mapperObjList.forEach {
    mapperConfig.insertMapper(it.uniqueId, it.script)
  }

  println(mapperConfig.evaluate(1))
  println(mapperConfig.evaluate(2))
  println(mapperConfig.evaluate(3))


}

data class MapperObj(
  var uniqueId: Long = 0L,
  var name: String = "",
  var script: String = ""
)


class MapperConfig {
  private val engineKts = ScriptEngineManager().getEngineByExtension("kts")

  init {
    engineKts.eval("assert(true)")
  }

  private val mapperFunctions: ConcurrentHashMap<Long, IntToInt>  = ConcurrentHashMap()

  fun insertMapper(uniqueId: Long, lambda: String) {
    val lambdaFunc  = engineKts.eval(lambda) as IntToInt
    mapperFunctions[uniqueId] = lambdaFunc
  }
  fun removeMapper(uniqueId: Long, lambda: String) {
  }

  fun evaluate(input: Int) : Map<Long, Int> {
    return mapperFunctions.map { it.key to it.value.invoke(input) }.toMap()
  }



}
