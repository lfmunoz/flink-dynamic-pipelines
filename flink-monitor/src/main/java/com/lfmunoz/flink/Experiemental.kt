package com.lfmunoz.flink

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import javax.script.ScriptEngineManager


fun main() {
  println("test")


  val engineKts = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine
  val comp1 = engineKts.compile("val x = 3")
  val comp2 = engineKts.compile("x + 2")

  runBlocking {

    flowOf("a", "b", "c")
      .onCompletion {
        emit("x")

      }
    .collect { println(it)}

  }

}
