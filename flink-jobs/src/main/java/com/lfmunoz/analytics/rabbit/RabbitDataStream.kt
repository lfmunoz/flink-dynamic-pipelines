package eco.analytics.bridge.rabbit

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.api.common.state.ListState
import org.apache.flink.api.common.state.ListStateDescriptor
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.FunctionInitializationContext
import org.apache.flink.runtime.state.FunctionSnapshotContext
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.connectors.rabbitmq.RMQSink
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig
import org.apache.flink.util.Collector

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