package com.lfmunoz.flink

import com.lfmunoz.flink.engine.DynamicMapperProcessFunction
import com.lfmunoz.flink.engine.MapperResult
import com.lfmunoz.flink.flink.FlinkUtils
import com.lfmunoz.flink.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.flink.flink.MapOfStringToString
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.kafka.kafkaSink
import com.lfmunoz.flink.kafka.kafkaSource
import com.lfmunoz.flink.monitor.MonitorMessage
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.common.typeinfo.TypeInformation

// Flink Job:  Kafka -> Mapper ->  Kafka
fun dynamicMapperJob(jobCtx: FlinkJobContext) {

  val broadcastMapperStream =  jobCtx.env.setParallelism(1)
    .kafkaSource(jobCtx.mapperKafka)
    .map { mapper.readValue(it.value, FlinkUtils.mapToStringOfStringType) as Map<String,String> }
    .returns(MapOfStringToString)
    .broadcast(mapperStateDescriptor)

  jobCtx.env
    .kafkaSource(jobCtx.inputKafka)
    .map { it.value }.returns(ByteArray::class.java)
    .map { mapper.readValue(it, MonitorMessage::class.java)}.returns(MonitorMessage::class.java)
    .connect(broadcastMapperStream)
    .process(DynamicMapperProcessFunction)
    .returns(MapperResult::class.java)
    .map {
      val key = mapper.writeValueAsBytes(it.id)
      val value = mapper.writeValueAsBytes(it)
      KafkaMessage(key, value)
    }
    .returns(KafkaMessage::class.java)
    .kafkaSink(jobCtx.outputKafka)

}

//________________________________________________________________________________
// BROADCAST MAPPER
//________________________________________________________________________________
val mapperStateDescriptor= MapStateDescriptor(
  "MapperConfig",
  BasicTypeInfo.STRING_TYPE_INFO,
  TypeInformation.of(MapOfStringToString)
)

