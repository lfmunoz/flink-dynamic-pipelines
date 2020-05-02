package com.lfmunoz.flink.rabbit

import org.apache.flink.api.common.serialization.AbstractDeserializationSchema
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.rabbitmq.RMQSink
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig

const val EXCHANGE_TYPE: String = "fanout"
const val EXCHANGE_DURABLE: Boolean = false

const val QUEUE_AUTO_DELETE: Boolean = true


//________________________________________________________________________________
// Rabbit Consumer
//________________________________________________________________________________
fun StreamExecutionEnvironment.rabbitSource(
        rabbitConfig: RabbitConfig
) : DataStream<ByteArray> {
    val aRMQConnectionConfig = RMQConnectionConfig.Builder()
            .setUri(rabbitConfig.amqp).build()
    return  this.addSource( OurRMQSource(
            rabbitConfig,
            aRMQConnectionConfig
    ), "rabbitConsumer[$rabbitConfig]")
}

class OurRMQSource (
    private val rabbitConfig: RabbitConfig,
    aRMQConnectionConfig: RMQConnectionConfig
): RMQSource<ByteArray>(
        aRMQConnectionConfig,
        rabbitConfig.queue,
        rabbitDeserializer
) {
    override fun setupQueue() {
        channel.exchangeDeclare(rabbitConfig.exchange, EXCHANGE_TYPE, EXCHANGE_DURABLE)
        val aDeclareOK = channel.queueDeclare(queueName, false,
                false, QUEUE_AUTO_DELETE, null)
        channel.queueBind(aDeclareOK.queue, rabbitConfig.exchange, "*")
    }
}

val rabbitDeserializer= object : AbstractDeserializationSchema<ByteArray>() {
    override fun deserialize(aBtyeArray: ByteArray): ByteArray{
        return aBtyeArray
    }
}

//________________________________________________________________________________
// Rabbit Publisher
//________________________________________________________________________________
fun DataStream<ByteArray>.rabbitSink(
    rabbitConfig: RabbitConfig
) : DataStreamSink<ByteArray> {
    val aRMQConnectionConfig = RMQConnectionConfig.Builder().setUri(rabbitConfig.amqp).build()
    return this.addSink( OurRMQSink(
            aRMQConnectionConfig,
            rabbitConfig
    )).name("rabbitPublisher[$rabbitConfig]")
}

fun SingleOutputStreamOperator<ByteArray>.rabbitSink(
        rabbitConfig: RabbitConfig
) : DataStreamSink<ByteArray> {
    val aRMQConnectionConfig = RMQConnectionConfig.Builder().setUri(rabbitConfig.amqp).build()
    return this.addSink( OurRMQSink(
            aRMQConnectionConfig,
            rabbitConfig
    )).name("rabbitPublisher[$rabbitConfig]")
}



class OurRMQSink(
        rmqConnectionConfig: RMQConnectionConfig,
        private val rabbitConfig: RabbitConfig
): RMQSink<ByteArray>(
        rmqConnectionConfig,
        rabbitConfig.queue,
        rabbitSerializer
) {
    override fun setupQueue() {
        channel.exchangeDeclare(rabbitConfig.exchange, EXCHANGE_TYPE, EXCHANGE_DURABLE)
        val aDeclareOK = channel.queueDeclare(queueName, false,
                false, QUEUE_AUTO_DELETE, null)
        channel.queueBind(aDeclareOK.queue, rabbitConfig.exchange, "*")
    }
}

val rabbitSerializer = SerializationSchema<ByteArray> { it }
