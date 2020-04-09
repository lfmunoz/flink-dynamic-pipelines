package eco.analytics.bridge

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import eco.analytics.bridge.flink.*
import eco.analytics.bridge.flink.FlinkUtils.Companion.listMonitorMessageType
import eco.analytics.bridge.flink.FlinkUtils.Companion.mapper
import eco.analytics.bridge.monitor.MonitorMessageDataGenerator
import eco.analytics.bridge.rabbit.RabbitConfig
import eco.analytics.bridge.rabbit.rabbitSink
import eco.analytics.bridge.rabbit.rabbitSource
import eco.analytics.flink.data.eco.MonitorMessage
import eco.analytics.flink.kafka.*
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.apache.kafka.common.metrics.stats.Count
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.forEach
import kotlin.collections.map

/**
 * Integration Test - Monitor Message Integration Test
 *  Dependencies: RabbitMQ / Kafka
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorMessageIntTest {

    @ClassRule
    var flinkCluster = MiniClusterWithClientResource(
            MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(2)
                    .setNumberTaskManagers(1)
                    .build())

    private val messageTotal = 100L
    private val parallelism = 3

    private val bootstrapServer =  System.getProperty("bootstrapServer", "localhost:9092")
    private val amqp = System.getProperty("ampq", "amqp://guest:guest@localhost:5672")


    //________________________________________________________________________________
    // Test Cases
    //________________________________________________________________________________
    @Test
    fun `SERIALIZE and DESERIALIZE List of MonitorMessage with Kryo`() {
        val kryo = Kryo()
        kryo.register(MonitorMessage::class.java)
        kryo.register(HashMap::class.java)
        kryo.register(ArrayList::class.java)
        val byteStream = ByteArrayOutputStream()
        val output = Output(byteStream)

        val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(10)
        val listOfObjects = (1..100).map {
            aMonitorMessageDataGenerator.random(true, 30, 30)
        }
        kryo.writeClassAndObject(output, listOfObjects)
        output.flush()

        val input = Input(ByteArrayInputStream(byteStream.toByteArray()))
        val readMonitorMessage = kryo.readClassAndObject(input) as List<MonitorMessage>

        assertThat(readMonitorMessage).isEqualTo(listOfObjects)
    }

    @Test
    fun `SERIALIZE and DESERIALIZE List Of MonitorMessage with Jackson`() {
        val aMonitorMessageDataGenerator = MonitorMessageDataGenerator(10)
        val listOfObjects = (1..100).map {
            aMonitorMessageDataGenerator.random(true, 30, 30)
        }
        val serialize = mapper.writeValueAsBytes(listOfObjects)
        val deserialize: List<MonitorMessage> = mapper.readValue(serialize, listMonitorMessageType)
        assertThat(listOfObjects).isEqualTo(deserialize)
    }

    @Test
    fun `MonitorMessage to Rabbit from Kafka to Kafka`() {
        val args = arrayOf("--remote", "false")
        val aFlinkJobContext = parseParameters(args)
        val aRabbitConfigPublish = RabbitConfig(
                amqp = amqp,
                queue = "monitor-rabbit-test",
                exchange = "monitor-rabbit-test"
        )
        val aKafkaConfigPublish = KafkaConfig(
                bootstrapServer,
                "lz4",
                "monitor-group-id",
                KafkaTopics("monitor-kafka-test", "monitor-kafka-test")
        )

        Thread() {
            // MONITOR MESSAGE -> RABBIT
            aFlinkJobContext.env.setParallelism(parallelism)
                    .addSource(FlinkMonitorMessageGenerator(messageTotal, 10L), "GradeGenerator")
                    .flatMap<ByteArray>(bufferToByteArray<MonitorMessage>(10)).returns(ByteArray::class.java)
                    .rabbitSink(aRabbitConfigPublish)

            //  RABBIT -> KAFKA
            aFlinkJobContext.env.setParallelism(parallelism)
                    .rabbitSource(aRabbitConfigPublish)
                    .map {
                        return@map  mapper.readValue(it, listMonitorMessageType) as List<MonitorMessage>
                    }.returns(ListMonitorMessageType)
                    .flatMap<KafkaMessage> { aMonitorMessageList, collector ->
                        aMonitorMessageList.forEach { aMonitorMessage ->
                            val key = aMonitorMessage.uuid?.toByteArray() ?: "".toByteArray()
                            val value = mapper.writeValueAsBytes(aMonitorMessage)
                            collector.collect(KafkaMessage(key, value))
                        }
                    }.returns(KafkaMessage::class.java)
                    .kafkaSink(aKafkaConfigPublish, "monitor-kafka-test")


            // KAFKA -> CHECKER
            aFlinkJobContext.env.setParallelism(parallelism)
                    .kafkaSource(aKafkaConfigPublish, "monitor-kafka-test")
                    .map {
                        return@map it.value
                    }.returns(ByteArray::class.java)
                    .addSink(CollectSink())

            aFlinkJobContext.env.execute()
        }.start()
        await.untilAsserted {
            assertThat(CollectSink.values.size).isEqualTo(messageTotal*parallelism)
        }
    }

    //________________________________________________________________________________
    // HELPER METHODS
    //________________________________________________________________________________
    private class CollectSink() : SinkFunction<ByteArray> {
        override fun invoke(value: ByteArray) {
            values.add(value)
        }
        companion object {
            // must be static
            val values: MutableList<ByteArray> = Collections.synchronizedList(ArrayList())
        }
    }

} // EOF
