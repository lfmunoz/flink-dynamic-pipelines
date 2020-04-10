package eco.analytics.bridge.rabbit

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.ClassRule
import java.util.ArrayList
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import eco.analytics.bridge.flink.*
import com.lfmunoz.analytics.flink.FlinkUtils.Companion.listIntType
import com.lfmunoz.analytics.flink.FlinkUtils.Companion.mapper
import com.lfmunoz.analytics.flink.ListIntType
import com.lfmunoz.analytics.flink.parseParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration Test - Rabbit Integration Test
 *  Dependencies: RabbitMQ
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitIntTest {
    @ClassRule
    var flinkCluster = MiniClusterWithClientResource(
            MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(2)
                    .setNumberTaskManagers(1)
                    .build())

    private val amqp = System.getProperty("ampq", "amqp://guest:guest@localhost:5672")


    //________________________________________________________________________________
    // Test Cases
    //________________________________________________________________________________
    @Test
    fun `SERIALIZE and DESERIALIZE Integer`() {
        val type = mapper.getTypeFactory().constructCollectionType(List::class.java, Int::class.java)
        val listOfIntegers = listOf(1, 2, 3)
        val serialize = mapper.writeValueAsBytes(listOfIntegers)
        val deserialize: List<Int> = mapper.readValue(serialize, type)
        assertThat(listOfIntegers).isEqualTo(deserialize)
    }

    @Test
    fun `Integer WRITE and READ to RABBIT MQ`() {
        val messageTotal = 100L
        val latch = CountDownLatch(1)
        val args = arrayOf("--remote", "false")
        val aFlinkJobContext = parseParameters(args)
        val aRabbitConfig = RabbitConfig(
                amqp = amqp,
                queue = "bridge-rabbit-test",
                exchange = "bridge-rabbit-test"
        )

        Thread() {
            // RABBIT WRITE
            aFlinkJobContext.env.setParallelism(1)
                    .addSource(FlinkIntGenerator(messageTotal, 10L), "GradeGenerator")
                    .flatMap(bufferToByteArray<Int>(10)).returns(ByteArray::class.java)
                    .rabbitSink(aRabbitConfig)

            // RABBIT READ
            aFlinkJobContext.env.setParallelism(1)
                    .rabbitSource(aRabbitConfig)
                    .map {
                        return@map mapper.readValue(it, listIntType) as List<Int>
                    }.returns(ListIntType)
                    .flatMap<Long> { integers , collector ->
                        integers.forEach { aInt ->
                            collector.collect(aInt.toString().toLong())
                        }
                    }.returns(Long::class.java)
                    .addSink(CollectSink())

            aFlinkJobContext.env.execute()
            latch.countDown()
        }.start()
        latch.await(6, TimeUnit.SECONDS)
        assertThat(CollectSink.values.size).isEqualTo(messageTotal)
    }

    //________________________________________________________________________________
    // Helper methods
    //________________________________________________________________________________
    // create a testing sink
    private class CollectSink : SinkFunction<Long> {
        @Synchronized override fun invoke(value: Long) { values.add(value) }
        companion object {
            // must be static
            val values: MutableList<Long> = ArrayList()
        }
    }

} // EOF
