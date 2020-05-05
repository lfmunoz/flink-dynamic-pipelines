package com.lfmunoz.monitor


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import net.sf.expectit.Expect

import java.io.File
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import java.util.concurrent.TimeUnit
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.matcher.Matchers.*
import org.fissore.slf4j.FluentLoggerFactory
import kotlin.coroutines.CoroutineContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe


/**
 * Service - BashService
 *
 *   Heavily dependent on Expecit:
 *     http://alexey1gavrilov.github.io/ExpectIt/0.9.0/apidocs/
 *
 */
@ThreadSafe
class BashService(
  private val maxTimeout: Long = 3000L
) {

  companion object {
    private val log = FluentLoggerFactory.getLogger(BashService::class.java)
    //    private const val CMD_LINE_EXE = "/bin/sh"
    private const val CMD_LINE_EXE = "/bin/bash"
    private const val EOF = "De1AdBeAF3" + "j01l_1"
    private const val NUMBER_OF_PROCESSES = 3
  }

  private val job = SupervisorJob()
  private val kotlinContext: List<CoroutineContext>
  private val scope: List<CoroutineScope>
  private val stdError: List<BufferedReader>
  @GuardedBy("this") private var index = 0
  private var latency: Long = 0

  private val builder = ProcessBuilder(CMD_LINE_EXE)  //.redirectErrorStream(true)
  private val expect: List<Expect>
  private val process: List<Process>

  var isAvailable: Boolean

  //________________________________________________________________________________
  // CONSTRUCTOR
  //________________________________________________________________________________
  init {
    kotlinContext = (1..NUMBER_OF_PROCESSES).map {
      newSingleThreadContext("bash-$it")
    }
    scope = kotlinContext.map {
      CoroutineScope(it + job)
    }
    process = (1..NUMBER_OF_PROCESSES).map { builder.start() }
    stdError = process.map {
      BufferedReader(InputStreamReader(it.errorStream))
    }
    expect = process.map {
      ExpectBuilder()
        .withTimeout(maxTimeout, TimeUnit.MILLISECONDS)
        .withInputs(it.inputStream)  //, process.getErrorStream())
        .withOutput(it.outputStream)
        .build()
    }
    isAvailable = File(CMD_LINE_EXE).canExecute()

  }

  //________________________________________________________________________________
  // PUBLIC
  //________________________________________________________________________________
  @Synchronized
  fun runCmd(cmd: String, timeout: Long = maxTimeout): Flow<CmdResult> {
    index += 1
    if (index == NUMBER_OF_PROCESSES) {
      index = 0
    }
    return flowBuilder(cmd, stdError[index], expect[index], timeout).flowOn(kotlinContext[index])
  }


  private suspend fun confirmConnection(): Boolean {
    val aCmdResult = runCmd("echo c0ckp1t", maxTimeout).toList()
    return aCmdResult.last() is CmdResult.Success && aCmdResult.first() is CmdResult.Stdout
  }


//  private fun flowBuilder2(command: String) {
//     val pb: ProcessBuilder = ProcessBuilder("myCommand", "myArg1", "myArg2");
//     pb.redirectErrorStream(true);
//     pb.redirectOutput(Redirect.appendTo(log));
//     val p: Process  = pb.start()
//  }

  //________________________________________________________________________________
  // PRIVATE
  //________________________________________________________________________________
  private fun flowBuilder(cmd: String, stdError: BufferedReader, expect: Expect, timeout: Long = maxTimeout): Flow<CmdResult> = flow {
    val start = System.currentTimeMillis()
    log.debug().log("[bash start] - time=$start - cmd=$cmd ")
//    expect.send("$cmd; echo $EOF")
    expect.send(cmd)
    expect.sendLine()
    expect.send("echo $EOF")
    expect.sendLine()
    while (true) {
      val result = expect.withTimeout(timeout, TimeUnit.MILLISECONDS).expect(contains("\n"))
      if (!result.isSuccessful) {
        resultTimeout(timeout, ::emit)
        break
      }
      if (result.before == EOF) {
        resultEOF(stdError, ::emit)
        break
      }
      resultOk(result.before, ::emit)
    }
    latency = System.currentTimeMillis() - start
    log.debug().log("[bash end] - elapse=${latency} - cmd=$cmd ")
  }

  private suspend fun resultOk(stdout: String, emit: suspend (CmdResult) -> Unit) {
    emit(CmdResult.Stdout(stdout))
  }

  private suspend fun resultTimeout(timeout: Long, emit: suspend (CmdResult) -> Unit) {
    emit(CmdResult.Timeout("Command didn't finish after ${TimeUnit.MILLISECONDS.toSeconds(timeout)} seconds"))
  }

  private suspend fun resultEOF(stdError: BufferedReader, emit: suspend (CmdResult) -> Unit) {
    val errorFirstLine = if (stdError.ready()) { stdError.readLine() } else { ""}
    if (!errorFirstLine.isBlank()) {
      emit(CmdResult.Stderr(errorFirstLine))
      while (stdError.ready()) {
        emit(CmdResult.Stderr(stdError.readLine()))
      }
      emit(CmdResult.Failure)
    } else {
      emit(CmdResult.Success)
    }
  }


  //________________________________________________________________________________
  // tearDown
  //________________________________________________________________________________
  fun tearDown() {
    job.cancel()
    process.forEach {
      it.destroy()
      it.waitFor()
    }
    expect.forEach {
      it.close()
    }
    isAvailable = false
  }

} // EOF



sealed class CmdResult {
  object Success: CmdResult()
  object Failure: CmdResult()
  data class Stdout(val line: String): CmdResult()
  data class Stderr(val line: String): CmdResult()
  data class Timeout(val message: String): CmdResult()
}
