package com.lfmunoz.flink.engine

import com.lfmunoz.flink.monitor.MonitorMessage
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.util.Collector

object DynamicMapperProcessFunction: BroadcastProcessFunction<MonitorMessage, Map<String, String>, MapperResult>() {
  override fun processElement(value: MonitorMessage, ctx: ReadOnlyContext, out: Collector<MapperResult>) {
    out.collect(MapperResult())
  }

  override fun processBroadcastElement(value: Map<String, String>, ctx: Context, out: Collector<MapperResult>) {
    println("[got config] - $value")
    DynamicEngine.update(value)
  }

}
