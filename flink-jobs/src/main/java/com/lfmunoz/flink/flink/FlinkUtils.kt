package com.lfmunoz.flink.flink

import com.fasterxml.jackson.databind.ObjectMapper
import com.lfmunoz.flink.kafka.KafkaMessage
import com.lfmunoz.flink.monitor.MonitorMessage
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
val MapOfStringToString = object : TypeHint<Map<String, String>>() {}

class FlinkUtils {
    companion object {
        val mapper = ObjectMapper()
        val listIntType = mapper.typeFactory.constructCollectionType(List::class.java, Int::class.java)
        val listMonitorMessageType = mapper.typeFactory.constructCollectionType(List::class.java, MonitorMessage::class.java)
        val listByteArrayType = mapper.typeFactory.constructCollectionType(List::class.java, ByteArray::class.java)
        val mapToStringOfStringType = mapper.typeFactory.constructMapType(Map::class.java, String::class.java, String::class.java)
    }
}



