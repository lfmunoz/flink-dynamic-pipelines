package com.lfmunoz.monitor.kafka

import com.lfmunoz.monitor.BashService
import com.lfmunoz.monitor.CmdResult
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

  suspend fun listTopics() : List<CmdResult> {
    val command = "kafka-topics.sh  --zookeeper $zookeeperUrl --list"
    return bash.runCmd("${commandPrefix}${command}").toList()
  }
  suspend fun describeTopic(topicName: String) : List<CmdResult> {
    val command = "kafka-topics.sh  --zookeeper $zookeeperUrl --describe --topic $topicName"
    return bash.runCmd("${commandPrefix}${command}").toList()
  }

  suspend fun listConsumerGroups() : List<CmdResult> {
    val command = "kafka-consumer-groups.sh  --bootstrap-server $bootstrapServer --list"
    return bash.runCmd("${commandPrefix}${command}").toList()
  }

  suspend fun describeConsumerGroup(groupId: String) :List<CmdResult> {
    val command = "kafka-consumer-groups.sh  --bootstrap-server $bootstrapServer --describe --group $groupId"
    return bash.runCmd("${commandPrefix}${command}").toList()
  }

  suspend fun diskUsage(topicName: String) :List<CmdResult> {
    val command = "kafka-log-dirs.sh --bootstrap-server $bootstrapServer --describe  --topic-list $topicName"
    return bash.runCmd("${commandPrefix}${command}").toList()
  }

//  docker exec -it kafka-${RND} bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list kafkaNet:9092 --topic mapper-topic
//docker exec -it kafka-${RND} bin/kafka-console-consumer.sh --bootstrap-server kafkaNet:9092 --topic mapper-topic --offset 0 --partition 0
//  docker exec -it kafka-${RND} bin/kafka-console-producer.sh --broker-list kafkaNet:9092 --topic my-topic

//  <confluent-path>/bin/kafka-console-consumer –topic amazingTopic -
//  - bootstrap-server localhost:9093 --new-consumer --consumer-
//  property
//  group.id=my-group


  suspend fun getLastMessage(topicName: String) : String {
    val getOffsetCmd = "kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list $bootstrapServer --topic $topicName"
    val offsetResult = bash.runCmd("${commandPrefix}${getOffsetCmd}")
      .filter{ it is CmdResult.Stdout}.map{ (it as CmdResult.Stdout).line }.toList()

    val partition  = offsetResult.first().split(":")[1].toInt()
    val offset = offsetResult.first().split(":")[2].toInt() - 1
    if(offset < 0) { return "no messages" } // no messages
    val getLastMessage = "kafka-console-consumer.sh --bootstrap-server $bootstrapServer  --topic $topicName --timeout-ms 4000 --max-messages 1 --offset $offset --partition $partition"
    val lastMessageResult = bash.runCmd("${commandPrefix}${getLastMessage}", 5_000L)
      .filter{ it is CmdResult.Stdout}.map{ (it as CmdResult.Stdout).line }
      .toList()
//    println(lastMessageResult)
    return lastMessageResult.first()
  }

}

// ________________________________________________________________________________
// EXPERIMENTAL
// ________________________________________________________________________________
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

