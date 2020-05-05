package com.lfmunoz.flink.flink

import org.apache.flink.api.common.functions.RichMapFunction
import java.util.concurrent.atomic.AtomicLong

//________________________________________________________________________________
// MISC
//________________________________________________________________________________
fun <T> take(
  count: Long = 100L
) : RichMapFunction<T, T> {
  return object: RichMapFunction<T, T>() {
    val numberOfSeenValues: AtomicLong = AtomicLong(0)
    override fun map(value: T): T {
      if(numberOfSeenValues.incrementAndGet() > count) {
        throw RuntimeException("[take completed ] - Took ${numberOfSeenValues.get()}")
      }
      println(numberOfSeenValues.get())
      return value
    }
  }
}
