package com.lfmunoz.flink.engine

import com.lfmunoz.flink.monitor.MonitorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.newSingleThreadContext
import org.fissore.slf4j.FluentLoggerFactory
import kotlin.reflect.KFunction
import kotlinx.coroutines.flow.emitAll
//import javax.annotation.concurrent.NotThreadSafe

/**
 * Base class for Kotlin Scripts
 */
//@NotThreadSafe
open class ScriptCtx(
  val key: String = "",
  val script: String = ""
) {

  protected val log = FluentLoggerFactory.getLogger(ScriptCtx::class.java)

  open fun main(m: MonitorMessage) : MapperResult  {
    log.info().log("[main]")
    return MapperResult()
  }

}


private val re = Regex("[^A-Za-z0-9]")

fun generateKts(key: String, userCode: String): String {
  val uniqueId= re.replace(key, "")
  return """
// KOTLIN
import kotlin.reflect.KFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emitAll

// FLINK
import com.lfmunoz.flink.engine.ScriptCtx
import com.lfmunoz.flink.monitor.MonitorMessage
import com.lfmunoz.flink.engine.MapperResult

// BINDINGS
val key = bindings["key"] as String
val script = bindings["script"] as String

//________________________________________________________________________________
// SCRIPT CLASS DEFINITION
//________________________________________________________________________________
class ${uniqueId.toUpperCase()}(
  _key: String,
  _script: String
) : ScriptCtx(_key, _script) {

  //________________________________________________________________________________
  // USER CODE
  //________________________________________________________________________________
  $userCode

} // END OF CLASS DEFINITION

//________________________________________________________________________________
// SCRIPT CLASS INSTANTIATION
//________________________________________________________________________________
val ${uniqueId.toLowerCase()} = ${uniqueId.toUpperCase()}(key, script)
${uniqueId.toLowerCase()}
""".trimIndent()
}
