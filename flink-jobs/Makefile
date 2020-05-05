#________________________________________________________________________________
#  CONFIG
#________________________________________________________________________________
.PHONY: build run

#________________________________________________________________________________
#  DEVELOPMENT
#________________________________________________________________________________

#________________________________________________________________________________
#  PRODUCTION
#________________________________________________________________________________
FLINK_HOST=loaclhost
FLINK_PORT=8081
# FLINK_JAR=rabbit-kafka-bridge/build/libs/rabbit-kafka-bridge-0.0.6-all.jar
FLINK_JAR=build/libs/rabbit-kafka-bridge-0.0.6-all.jar

BOOTSTRAP_SERVER=10.37.240.46:9094
KAFKA_TOPIC=test-monitor-flink

AMPQ=amqp://guest:guest@10.37.240.51:5672
EXCHANGE=test-monitor-exchange
QUEUE=test-monitor-queue

SINK_EXCHANGE=test-monitor-exchange-sink
SINK_QUEUE=test-monitor-queue-sink

clean:
	../gradlew  :rabbit-kafka-bridge:clean

build:
	../gradlew  :rabbit-kafka-bridge:shadowJar

# monitor messages --> rabbit
run-publish:
	java -jar ${FLINK_JAR} --job RABBIT_PUBLISH \
	--remote true \
	--flinkHost ${FLINK_HOST} \
	--flinkPort ${FLINK_PORT} \
	--bootstrapServer ${BOOTSTRAP_SERVER} \
	--kafkaTopic ${KAFKA_TOPIC} \
	--amqp ${AMPQ} \
	--queue ${QUEUE} \
	--exchange ${EXCHANGE}

# rabbit --> kafka
run-rabbit:
	java -jar ${FLINK_JAR} --job RABBIT_TO_KAFKA \
	--remote true \
	--flinkHost ${FLINK_HOST} \
	--flinkPort ${FLINK_PORT} \
	--bootstrapServer ${BOOTSTRAP_SERVER} \
	--kafkaTopic ${KAFKA_TOPIC} \
	--amqp ${AMPQ} \
	--queue ${QUEUE} \
	--exchange ${EXCHANGE}

# kafka --> rabbit
run-kafka:
	java -jar ${FLINK_JAR} --job KAFKA_TO_RABBIT \
	--remote true \
	--flinkHost ${FLINK_HOST} \
	--flinkPort ${FLINK_PORT} \
	--bootstrapServer ${BOOTSTRAP_SERVER} \
	--kafkaTopic ${KAFKA_TOPIC} \
	--amqp ${AMPQ} \
	--queue ${SINK_QUEUE} \
	--exchange ${SINK_EXCHANGE}

#________________________________________________________________________________
# DOCKER NETWORK
#________________________________________________________________________________
NETWORK=flink-bridge
dependencies:
	docker network create -d bridge ${NETWORK}
	docker network ls
	docker network inspect ${NETWORK}

alpine:
	 docker run --rm --name alpine_linux --network=${NETWORK} -it  alpine /bin/sh

docker-up: zookeeper-start rabbit-start kafka-start

docker-down: zookeeper-stop rabbit-stop kafka-stop

#________________________________________________________________________________
#  FLINK
#  PUBLIC
#   http://localhost:8081
#
#  PRIVATE
#   JobManager RPC port 6123
#   TaskManagers RPC port 6122
#   TaskManagers Data port 6121
#________________________________________________________________________________
FLINK_OFFICIAL_IMAGE=flink:1.8.2-scala_2.12
FLINK_JOB_JAR=flink-job.jar
FLINK_HOSTNAME=localhost:8081

flink-start:
	 docker run --rm -d --name jobmanager -p  8081:8081 --network=${NETWORK} -e JOB_MANAGER_RPC_ADDRESS=jobmanager  ${FLINK_OFFICIAL_IMAGE} jobmanager
	 docker run --rm -d --name taskmanager_0  --network=${NETWORK}  -e JOB_MANAGER_RPC_ADDRESS=jobmanager ${FLINK_OFFICIAL_IMAGE} taskmanager
# 	 docker run --rm -d --name taskmanager_1  --network=${NETWORK}  -e JOB_MANAGER_RPC_ADDRESS=jobmanager ${FLINK_OFFICIAL_IMAGE} taskmanager
# 	 docker run --rm -d --name taskmanager_2  --network=${NETWORK}  -e JOB_MANAGER_RPC_ADDRESS=jobmanager ${FLINK_OFFICIAL_IMAGE} taskmanager

flink-stop:
	 docker stop jobmanager
	 docker stop taskmanage

flink-attach:
	 docker exec -it jobmanager bash

# https://github.com/apache/flink/pull/3722/files
flink-upload:
	 curl -X POST -H "Expect:" -F "jarfile=${FLINK_JOB_JAR}" http://${FLINK_HOSTNAME}/jars/upload

#________________________________________________________________________________
# Zookepper
#  Public port 2181 2888 3888 8091  (2181 is cconnection port)
#________________________________________________________________________________
zookeeper-start:
	 docker run -d --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 -p 8091:8080 --network=${NETWORK} -e "ZOO_4LW_COMMANDS_WHITELIST=*" zookeeper:3.5.5

zookeeper-stop:
	docker stop zookeeper

zookeeper-log:
	docker logs zookeeper

#________________________________________________________________________________
# Kafka
#  Public port 9092 (bootstrapServer)
#________________________________________________________________________________
# KAFKA_ADVERTISED_HOST_NAME=10.37.120.197
KAFKA_ADVERTISED_HOST_NAME=192.168.0.101

kafka-start:
	docker run -d --rm --name kafka -p 9092:9092 --network=${NETWORK} -e "ZOOKEEPER_CONNECT=zookeeper:2181" -e "ADVERTISED_HOST_NAME=${KAFKA_ADVERTISED_HOST_NAME}" debezium/kafka:1.0

kafka-stop:
	docker stop kafka

#________________________________________________________________________________
# RabbitMQ
# #  Public port 5672  15672
#________________________________________________________________________________
rabbit-start:
	docker run --name rabbitmq-collect --rm --network=${NETWORK} -p 5672:5672 -p 15672:15672 -d rabbitmq:3.7.2-management

rabbit-stop:
	docker stop rabbitmq-collect

#________________________________________________________________________________
# Grafana / Prometheus
#  Prometheus exposes 9090
#  Grafana exposes 3000
#________________________________________________________________________________
grafana-start:
	docker run -d --rm --name grafana --network=${NETWORK} -p 3000:3000 grafana/grafana

prometheus-start:
	docker run -d --rm --name prometheus --network=${NETWORK} -p 9090:9090 -v ~/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus
