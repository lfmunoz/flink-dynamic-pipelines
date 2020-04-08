package com.lfmunoz.flink.test

import com.lfmunoz.flink.web.ActionInterface
import com.lfmunoz.flink.web.WsPacket
import kotlinx.coroutines.flow.*
import org.fissore.slf4j.FluentLoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * Component - TestActoin
 */
class TestAction: ActionInterface {

  companion object {
    private val Log = FluentLoggerFactory.getLogger(TestAction::class.java)
  }

  // ________________________________________________________________________________
  // accept()
  // ________________________________________________________________________________
  override fun accept(wsPacket: WsPacket): Flow<String> {
    return flow<String> {
      val dto = TestDTO.fromJson(wsPacket.payload)
      val latency = Duration.between(Instant.now(), dto.created )
      Log.info().log("TestAction - {} - {}", latency.toMillis(), dto)
      if(dto.count == 0) {
        throw IllegalArgumentException("Count of 0 is invalid argument")
      }
      repeat(dto.count) {
        emit(buildResponse(it))
      }
    }
  }

  // ________________________________________________________________________________
  // Helper Methods
  // ________________________________________________________________________________
  private fun buildResponse(id: Int): String {
    return TestDTO(
      id = id.toLong(),
      created = Instant.now(),
      count = 0,
      message = "response: ${id}"
    ).toJson()
  }


} // EOF
