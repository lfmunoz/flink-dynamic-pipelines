package com.lfmunoz.monitor.kafka

import com.lfmunoz.monitor.BashService
import kotlinx.coroutines.flow.toList
import org.I0Itec.zkclient.ZkClient
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.Config
import org.apache.kafka.clients.admin.DescribeConfigsResult
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.config.ConfigResource
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooKeeper
import org.fissore.slf4j.FluentLoggerFactory
import org.junit.jupiter.api.Test
import java.util.*


class KafkaAdminBash(
  private val bash: BashService // TODO: COMMAND SERVICE
) {

  companion object {
    private val log = FluentLoggerFactory.getLogger(KafkaAdminBare::class.java)
  }

  private val zookeeperUrl = "zooKeeperNet:2181"
  private val bootstrapServer = "kafkaNet:9092"
  private val commandPrefix =  "docker exec kafka-12345678 bin/"

  suspend fun listTopics() : List<String> {
    val command = "kafka-topics.sh  --zookeeper $zookeeperUrl --list"
    return bash.runCmd("${commandPrefix}${command}").toList().map { it.toString() }
  }
  suspend fun describeTopic(topicName: String) : List<String> {
    val command = "kafka-topics.sh  --zookeeper $zookeeperUrl --describe --topic $topicName"
    return bash.runCmd("${commandPrefix}${command}").toList().map { it.toString() }
  }

  suspend fun listConsumerGroups() : List<String> {
    val command = "kafka-consumer-groups.sh  --bootstrap-server $bootstrapServer --list"
    return bash.runCmd("${commandPrefix}${command}").toList().map { it.toString() }
  }

  suspend fun describeConsumerGroup(groupId: String) :List<String> {
    val command = "kafka-consumer-groups.sh  --bootstrap-server $bootstrapServer --describe --group $groupId"
    return bash.runCmd("${commandPrefix}${command}").toList().map { it.toString() }
  }

  suspend fun diskUsage(topicName: String) :List<String> {
    val command = "kafka-log-dirs.sh --bootstrap-server $bootstrapServer --describe  --topic-list $topicName"
    return bash.runCmd("${commandPrefix}${command}").toList().map { it.toString() }
  }


}


class KafkaAdminBare(
  private val aKafkaAdminConfig: KafkaAdminConfig
) {

  companion object {
    private val log = FluentLoggerFactory.getLogger(KafkaAdminBare::class.java)
  }


  private val config = Properties().apply {
    put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, aKafkaAdminConfig.bootStrapServer)
  }

  // ________________________________________________________________________________
  // KAFKA ADMIN CLIENT
  // ________________________________________________________________________________
  fun listTopics() : List<String> {
    return AdminClient.create(config).use {
      it.listTopics().names().get().toList()
    }
  }

  fun getTopicConfig(topicName: String) : String {
    return AdminClient.create(config).use {
      val resource = ConfigResource(ConfigResource.Type.TOPIC, topicName)
      val describeConfigsResult: DescribeConfigsResult = it.describeConfigs(Collections.singleton(resource))
      val topicConfig: Map<ConfigResource, Config> = describeConfigsResult.all().get()
      return topicConfig.toString()
    }
  }

  fun listConsumerGroups() : List<String> {
    AdminClient.create(config).use {
      val groups = it.listConsumerGroups().all().get()
      return groups.map{ group -> group.groupId() }
    }
  }




}

class ZooKeeperAdmin(
  private val aKafkaAdminConfig: KafkaAdminConfig
) {
  companion object {
    private val log = FluentLoggerFactory.getLogger(ZooKeeperAdmin::class.java)
  }

  init {
    val zkClient = ZooKeeper(aKafkaAdminConfig.zooKeeperUrl, 10000, Watcher {
      log.info().log(it.toString())
    })
  }

  private val zkClient: ZooKeeper? = null

  fun listBrokers(): List<String> {
    if (zkClient == null) return emptyList()
    val ids = zkClient.getChildren("/brokers/ids", false).toList()
    return ids.map {
      String(zkClient.getData("/brokers/ids/$it", false, null))
    }
  }

  fun listTopics(): List<String> {
    if (zkClient == null) return emptyList()
    return zkClient.getChildren("/brokers/topics", false).toList()
  }

  fun listPartitions(topic: String): List<String> {
    if (zkClient == null) return emptyList()
    val path = "/brokers/topics/$topic/partitions"
    return if (zkClient.exists(path, false) != null) {
      zkClient.getChildren(path, false).toList()
    } else {
      emptyList()
    }
  }
}


data class KafkaAdminConfig(
  var zooKeeperUrl: String? = "localhost:2181",
  var bootStrapServer: String? = "localhost:2181"
)

