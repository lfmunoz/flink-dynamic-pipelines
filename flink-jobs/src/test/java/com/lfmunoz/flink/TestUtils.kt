package com.lfmunoz.flink

import org.apache.flink.streaming.api.functions.sink.SinkFunction
import java.util.*
import kotlin.collections.ArrayList

class CollectSink() : SinkFunction<ByteArray> {
  override fun invoke(value: ByteArray) {
    values.add(value)
  }

  companion object {
    // must be static
    val values: MutableList<ByteArray> = Collections.synchronizedList(ArrayList())
  }
}

