package com.lfmunoz.flink.flink

import com.lfmunoz.flink.kafka.KafkaConfig
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.monitor.MonitorMessage
import com.lfmunoz.flink.rabbit.RabbitConfig
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment


//________________________________________________________________________________
// FLINK CONFIG
//________________________________________________________________________________
data class FlinkConfig(
        var flinkHost: String = "",
        var flinkPort: Int = 0,
        var uberJar: String = ""
) : java.io.Serializable {
    constructor(params: ParameterTool) : this(
            params.get("flinkHost", "localhost"),
            params.getInt("flinkPort", 8081),
            params.get("uberJar", "build/libs/rabbit-kafka-bridge-0.0.6-all.jar")
    )

}

// __________________________________________________________________________
// FLINK CONTEXT
//________________________________________________________________________________
data class FlinkJobContext(
  val env: StreamExecutionEnvironment,
  val rabbitConfig: RabbitConfig,
  val kafkaConfig: KafkaConfig,
  val flinkConfig: FlinkConfig,
  val job: String
)

enum class AvailableJobs(value: String) {
    KAFKA_TO_RABBIT("KAFKA_TO_RABBIT"),
    RABBIT_TO_KAFKA("RABBIT_TO_KAFKA"),
    RABBIT_PUBLISH("RABBIT_PUBLISH")
}

fun parseParameters(args: Array<String>): FlinkJobContext {
    val params = ParameterTool.fromArgs(args)
    val aRabbitConfig = RabbitConfig(params)
    val aKafkaConfig = KafkaConfig(params)
    val aFlinkConfig = FlinkConfig(params)
    val job = params.get("job", "INVALID_JOB")
    val env = createFlinkEnv(params, aFlinkConfig)
    return FlinkJobContext(env, aRabbitConfig, aKafkaConfig, aFlinkConfig, job)
}

fun createFlinkEnv(params: ParameterTool, aFlinkConfig: FlinkConfig): StreamExecutionEnvironment {
    val remote = params.getBoolean("remote", false)
    return if (remote) {
        StreamExecutionEnvironment.createRemoteEnvironment(aFlinkConfig.flinkHost, aFlinkConfig.flinkPort, aFlinkConfig.uberJar)
    } else {
        StreamExecutionEnvironment.getExecutionEnvironment()
    }
}


//________________________________________________________________________________
// COMMON FLINK DATA TYPES
//________________________________________________________________________________
val ListByteArrayType = object : TypeHint<List<ByteArray>>() {}
val ListKafkaMessageType = object : TypeHint<List<KafkaMessage>>() {}
val ListIntType = object : TypeHint<List<Int>>() {}
val ListStringType = object : TypeHint<List<String>>() {}
val ListMonitorMessageType = object : TypeHint<List<MonitorMessage>>() {}

class FlinkUtils {
    companion object {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        val listIntType = mapper.typeFactory.constructCollectionType(List::class.java, Int::class.java)
        val listMonitorMessageType = mapper.typeFactory.constructCollectionType(List::class.java, MonitorMessage::class.java)
        val listByteArrayType = mapper.typeFactory.constructCollectionType(List::class.java, ByteArray::class.java)
    }
}



