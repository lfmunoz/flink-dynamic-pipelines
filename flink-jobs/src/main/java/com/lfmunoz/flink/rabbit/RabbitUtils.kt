package com.lfmunoz.flink.rabbit

import org.apache.flink.api.java.utils.ParameterTool

//________________________________________________________________________________
// RABBIT CONFIG
//________________________________________________________________________________
data class RabbitConfig(
        var amqp: String = "",
        var queue: String = "",
        var exchange: String = "",
        var bufferSize: Int = 100
) : java.io.Serializable {
    constructor(params: ParameterTool) : this(
            params.get("amqp", "amqp://guest:guest@localhost:5672"),
            params.get("queue", "flink-rabbit-consumer"),
            params.get("exchange", "flink-rabbit-exchange"),
            params.getInt("bufferSize", 100)
    )
}

