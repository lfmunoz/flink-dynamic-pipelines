package com.lfmunoz.flink.engine


import com.lfmunoz.flink.monitor.MonitorMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DynamicEngineUnitTest {

  val mapper1 = """
  override open fun main(m: MonitorMessage) : MapperResult  {
    log.info().log("[main]")
    return MapperResult(values= mutableMapOf("two" to "2"))
  }
  """.trimIndent()

  val mapper2 = """
  override open fun main(m: MonitorMessage) : MapperResult  {
    log.info().log("[main]")
    return MapperResult(values= mutableMapOf("one" to "1"))
  }
  """.trimIndent()

  val mapper3 = """
  override open fun main(m: MonitorMessage) : MapperResult  {
    log.info().log("[main]")
    return MapperResult(values= mutableMapOf("ten" to "10", "six" to "6"))
  }
  """.trimIndent()


  @Test
  fun `basic functionality`() {

    DynamicEngine.insert("key1", mapper1)
    DynamicEngine.insert("key2", mapper2)
    DynamicEngine.insert("key3", mapper3)
    val result = DynamicEngine.evaluate(MonitorMessage())
    println(result)

  }

  val sandBox: MonitorLambda = lambda@{ m: MonitorMessage ->
    return@lambda MapperResult()
  }

  @Test
  fun `sandBox should work`() {
    println(sandBox(MonitorMessage()))
  }


}

