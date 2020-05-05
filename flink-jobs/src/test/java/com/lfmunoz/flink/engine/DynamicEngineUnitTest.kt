package com.lfmunoz.flink.engine


import com.lfmunoz.flink.monitor.MonitorMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DynamicEngineUnitTest {


  @Test
  fun `basic functionality`() {
    val mapper1 = """
      { m: MonitorMessage ->
      System.currentTimeMillis().toString()
     }""".trimMargin()

    val mapper2 = """{ m: MonitorMessage -> "return2" }"""
    DynamicEngine.insert("key1", mapper1)
    DynamicEngine.insert("key2", mapper2)
    val result = DynamicEngine.evaluate(MonitorMessage())
    println(result)

  }

  val mapper1X: MonitorLambda = lambda@ { m: MonitorMessage ->
    return@lambda MapperResult(values = mutableMapOf("key1" to "return2"))
  }
  val mapper2Z : MonitorLambda = lambda@ { m: MonitorMessage ->
    return@lambda MapperResult(values = mutableMapOf("key2" to "return2"))
  }

  @Test
  fun `lambda should work`() {
    println(mapper1X(MonitorMessage()))
  }


}

