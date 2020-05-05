#________________________________________________________________________________
# DEFAULT
#________________________________________________________________________________
USER = $(shell whoami)
USER_ID = $(shell id -u ${USER})
DIR = ${CURDIR}
RND ?= 12345678
NETWORK ?= flink-bridge-${RND}

KAFKA_IP ?= 192.168.0.101

RABBIT_PORT ?= -p 5672:5672 -p 15672:15672
KAFKA_PORT ?= -p 9092:9092
ZOOKEEPER_PORT ?= -p 2181:2181 -p 2888:2888 -p 3888:3888 -p 8091:8080
ZOOKEEPER_UI_PORT ?= -p 9090:9090

default:
	@echo ${NETWORK}
	@echo ${CURDIR}
	@echo ${USER}
	@echo ${USER_ID}
	@echo ${RND}


#________________________________________________________________________________
# DEVELOPMENT
#________________________________________________________________________________
.PHONY: unit now dev clean package

clean:
	./gradlew clean

run:
	./gradlew run

unit:
	./gradlew test --tests *UnitTest*

int:
	./gradlew test --tests *IntTest*

test:
	./gradlew test



#________________________________________________________________________________
# PRODUCTION
#________________________________________________________________________________
frontend:
	@echo "frontend"
	cd webapp ; npm install
	cd webapp ; npm run build
	rm -rf flink-monitor/src/main/resources/static/*
	cp -r webapp/dist/* flink-monitor/src/main/resources/static


package:
	./gradlew assemble -x test -Pprod -PVERSION=${VERSION} --no-daemon


#________________________________________________________________________________
# Docker Start
#________________________________________________________________________________
.PHONY: docker-start network-start zookeeper-start kafka-start rabbit-start
.PHONY: docker-stop rabbit-stop kafka-stop zookeeper-stop network-stop
docker-start: network-start zookeeper-start kafka-start rabbit-start
docker-stop: rabbit-stop kafka-stop zookeeper-stop network-stop

#________________________________________________________________________________
# DOCKER NETWORK
#  SUBNET: "172.18.0.0/16"
#  GATEWAY: "172.18.0.1"
#________________________________________________________________________________
network-start:
	-docker network create -d bridge ${NETWORK}

network-stop:
	-docker network rm ${NETWORK}

#________________________________________________________________________________
# RUN TEST IN CONTAINER
#________________________________________________________________________________
test:
	docker run --rm -it --name test-${RND} --network ${NETWORK} \
	    -u  id -u ${USER}):$(id -g ${USER})
	    -e GRADLE_USER_HOME=.gradle \
	    -v ${DIR}:/project \
	    -v ~/.gradle/gradle.properties:/project/.gradle/gradle.properties  \
	    -w /project \
	    openjdk:8  bash
	docker stop test-${RND}

#________________________________________________________________________________
# Zookepper
#  Public port 2181 2888 3888 8091  (2181 is cconnection port)
#  Public port
# IP: 172.18.0.6
#________________________________________________________________________________
zookeeper-start:
	-docker run -d --rm --name zookeeper-${RND} --network=${NETWORK} --network-alias=zooKeeperNet ${ZOOKEEPER_PORT} \
	    -e "ZOO_4LW_COMMANDS_WHITELIST=*" \
	    zookeeper:3.5.5

zookeeper-stop:
	-docker stop zookeeper-${RND}

zoo-ui-start:
	docker run -d --rm --name zkui ${ZOOKEEPER_UI_PORT} --network=${NETWORK} -e ZK_SERVER=zooKeeperNet:2181 qnib/plain-zkui

zoo-ui-stop:
	docker stop zkui

#________________________________________________________________________________
# RabbitMQ
# #  Public port 5672  15672
# IP: 172.18.0.7
#________________________________________________________________________________
rabbit-start:
	-docker run -d --rm --name rabbitmq-${RND} --network=${NETWORK} ${RABBIT_PORT} --network-alias=rabbitNet rabbitmq:3.7.2-management

rabbit-stop:
	-docker stop rabbitmq-${RND}

#________________________________________________________________________________
# Kafka (version = 2.12-2.4.0)
#  Public port 9092 (bootstrapServer)
# IP: 172.18.0.8
#________________________________________________________________________________
kafka-start:
	-docker run -d --rm --name kafka-${RND} --network=${NETWORK} ${KAFKA_PORT} --network-alias=kafkaNet \
	    -e "ADVERTISED_HOST_NAME=${KAFKA_IP}" \
	    -e "ZOOKEEPER_CONNECT=zookeeper-${RND}:2181" \
	    debezium/kafka:1.0

kafka-stop:
	-docker stop kafka-${RND}

# onnect-distributed.sh        kafka-configs.sh             kafka-delegation-tokens.sh  kafka-mirror-maker.sh                kafka-run-class.sh                  kafka-verifiable-consumer.sh     zookeeper-server-start.sh
# connect-mirror-maker.sh       kafka-console-consumer.sh    kafka-delete-records.sh     kafka-preferred-replica-election.sh  kafka-server-start.sh               kafka-verifiable-producer.sh     zookeeper-server-stop.sh
# connect-standalone.sh         kafka-console-producer.sh    kafka-dump-log.sh           kafka-producer-perf-test.sh          kafka-server-stop.sh                trogdor.sh                       zookeeper-shell.sh
# kafka-acls.sh                 kafka-consumer-groups.sh     kafka-leader-election.sh    kafka-reassign-partitions.sh         kafka-streams-application-reset.sh  windows
# kafka-broker-api-versions.sh  kafka-consumer-perf-test.sh  kafka-log-dirs.sh           kafka-replica-verification.sh        kafka-topics.sh                     zookeeper-security-migration.sh

# current-offset is the last committed offset of the consumer instance
# log-end-offset is the highest offset of the partition (hence, summing this column gives you the total number of messages for the topic)
# lag is the difference between the current consumer offset and the highest offset, hence how far behind the consumer is,
# owner is the client.id of the consumer (if not specified, a default one is displayed).

test:
# 	 docker exec -it kafka-${RND} bin/kafka-consumer-groups.sh  --bootstrap-server kafkaNet:9092 --list
# 	 docker exec -it kafka-${RND} bin/kafka-consumer-groups.sh  --bootstrap-server kafkaNet:9092 --describe --group octopus
# 	 docker exec -it kafka-${RND} bin/kafka-topics.sh  --zookeeper zooKeeperNet:2181 --list
# 	 docker exec -it kafka-${RND} bin/kafka-topics.sh  --zookeeper zooKeeperNet:2181 --describe --topic test-collect_mm
# 	 docker exec -it kafka-${RND} bin/kafka-log-dirs.sh --bootstrap-server kafkaNet:9092 --describe  --topic-list test-collect_mm
	 docker exec -it kafka-${RND} bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list kafkaNet:9092 --topic mapper-topic
# 	 docker exec -it kafka-${RND} bin/kafka-console-consumer.sh --bootstrap-server kafkaNet:9092 --topic mapper-topic --offset 0 --partition 0


prod:
	 docker exec -it kafka-${RND} bin/kafka-console-producer.sh --broker-list kafkaNet:9092 --topic my-topic

