package com.lfmunoz.flink


import com.lfmunoz.flink.flink.FlinkConfig
import com.lfmunoz.flink.flink.createFlinkEnv
import com.lfmunoz.flink.kafka.KafkaConfig
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.slf4j.LoggerFactory


// __________________________________________________________________________
// MAIN FLINK JOB ENTRY POINT
//________________________________________________________________________________
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("FlinkMainAppKt")
    val jobCtx = parseParameters(args)
    log.info("Flink Application started")
    log.info("[config] - ${jobCtx}")
    dynamicMapperJob(jobCtx)
}

// __________________________________________________________________________
// FLINK CONTEXT
//________________________________________________________________________________
data class FlinkJobContext(
  val env: StreamExecutionEnvironment,
  val inputKafka: KafkaConfig,
  val mapperKafka: KafkaConfig,
  val outputKafka: KafkaConfig,
  val flinkConfig: FlinkConfig
)

fun parseParameters(args: Array<String>): FlinkJobContext {
  val params = ParameterTool.fromArgs(args)
  val inputKafka = KafkaConfig(params).apply {
    bootstrapServer = params.get("bootstrapServer", "localhost:9092")
    kafkaTopic =  params.get("inputTopic", "input-topic")
  }
  val mapperKafka = KafkaConfig().apply {
    bootstrapServer = params.get("bootstrapServer", "localhost:9092")
    kafkaTopic =  params.get("mapperTopic", "mapper-topic")
  }
  val outputKafka = KafkaConfig(params).apply {
    bootstrapServer = params.get("bootstrapServer", "localhost:9092")
    kafkaTopic =  params.get("outputTopic", "output-topic")

  }
  val flinkConfig = FlinkConfig(params)
  val env = createFlinkEnv(params, flinkConfig)
  return FlinkJobContext(env, inputKafka, mapperKafka, outputKafka, flinkConfig)
}
