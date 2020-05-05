package com.lfmunoz.monitor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import javax.script.ScriptEngineManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.newSingleThreadContext


val context = newSingleThreadContext("t0")

fun mainX() {
  println("test")


  val engineKts = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine
  val comp1 = engineKts.compile("val x = 3")
  val comp2 = engineKts.compile("x + 2")

  runBlocking {

    flowOf("a", "b", "c")
      .onCompletion {
        emit("x")

      }
      .collect { println(it) }

  }

}

fun main() {
  val flow = flow {
    var i = 0
    while (true) {
      delay(1000)
      println("[${Thread.currentThread()}] Emit $i")
      emit(i++)
    }
  }.flowOn(context)

  runBlocking {
    flow.debounce(1000).collect {
      println("[${Thread.currentThread()}] GOT $it")
    }
  }

}


