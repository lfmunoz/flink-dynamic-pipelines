package eco.analytics.bridge

import eco.analytics.bridge.flink.AvailableJobs
import eco.analytics.bridge.flink.parseParameters
import org.slf4j.LoggerFactory

// Flink Job
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("FlinkMainAppKt")
    val jobCtx = parseParameters(args)
    log.info("Flink Application started")
    log.info("[config] - ${jobCtx}")
    when(jobCtx.job) {
        AvailableJobs.KAFKA_TO_RABBIT.name -> kafkaToRabbitBridgeJob(jobCtx)
        AvailableJobs.RABBIT_PUBLISH.name -> rabbitPublisherJob(jobCtx)
        AvailableJobs.RABBIT_TO_KAFKA.name -> rabbitToKafkaBridgeJob(jobCtx)
        else -> log.error("[invalid job] - ${jobCtx.job}")
    }
}

