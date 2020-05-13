#________________________________________________________________________________
# DEFAULT (ENVIRONMENT)
#________________________________________________________________________________
USER = $(shell whoami)
USER_ID = $(shell id -u ${USER})
GROUP_ID = $(shell id -g ${USER})
DIR = ${CURDIR}
RND ?= 12345678
NETWORK ?= flink-bridge-${RND}

KAFKA_IP ?= 192.168.0.101
KAFKA_PORT ?= -p 9092:9092
ZOOKEEPER_PORT ?= -p 2181:2181 -p 2888:2888 -p 3888:3888 -p 8091:8080
ZOOKEEPER_UI_PORT ?= -p 9090:9090

VERSION = 1.0.0

default:
	@echo ${NETWORK}
	@echo ${CURDIR}
	@echo ${USER}
	@echo ${USER_ID}
	@echo ${RND}
	-docker -v
	-java -version
	-node -v

#________________________________________________________________________________
# DEVELOPMENT
#________________________________________________________________________________
.PHONY: unit now dev clean package docker-test

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

test-ci:
	./gradlew test -Pjenkins

docker-test:
	docker run --rm -it --name test-${RND} --network ${NETWORK} \
	    -u ${USER_ID}:${GROUP_ID} \
	    -v ${DIR}:/project \
	    -e GRADLE_USER_HOME=.gradle \
	    -v ~/.gradle:/.gradle
	    -w /project \
	    openjdk:8 ./gradlew test --tests *UnitTest*d

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
.PHONY: docker-start network-start zookeeper-start kafka-start
.PHONY: docker-stop kafka-stop zookeeper-stop network-stop
docker-start: network-start zookeeper-start kafka-start
docker-stop: kafka-stop zookeeper-stop network-stop

#________________________________________________________________________________
# DOCKER NETWORK
#________________________________________________________________________________
network-start:
	-docker network create -d bridge ${NETWORK}
	-docker ps

network-stop:
	-docker network rm ${NETWORK}
	-docker ps


#________________________________________________________________________________
# Zookepper
#  Public port 2181 2888 3888 8091  (2181 is connection port)
#________________________________________________________________________________
zookeeper-start:
	-docker run -d --rm --name zookeeper-${RND} --network=${NETWORK} --network-alias=zooKeeperNet ${ZOOKEEPER_PORT} \
	    -e "ZOO_4LW_COMMANDS_WHITELIST=*" \
	    zookeeper:3.5.5

# zookeeper-start:
# 	-docker run -d --rm --name zookeeper-${RND} --network=${NETWORK} --network-alias=zooKeeperNet ${ZOOKEEPER_PORT} \
# 	    -e "ZOOKEEPER_CLIENT_PORT=2181" \
# 	    -e "ZOOKEEPER_TICK_TIME=2000" \
# 	    confluentinc/cp-zookeeper:5.4.0

zookeeper-stop:
	-docker stop zookeeper-${RND}


zoo-ui-start:
	docker run -d --rm --name zkui ${ZOOKEEPER_UI_PORT} --network=${NETWORK} -e ZK_SERVER=zooKeeperNet:2181 qnib/plain-zkui

zoo-ui-stop:
	docker stop zkui

#________________________________________________________________________________
# Kafka (version = 2.12-2.4.0)
#  Public port 9092 (bootstrapServer)
# 	    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafkaNet:29092,PLAINTEXT_HOST://localhost:9092 \
#________________________________________________________________________________
kafka-start:
	-docker run -d --rm --name kafka-${RND} --network=${NETWORK} ${KAFKA_PORT} --network-alias=kafkaNet \
	    -e "ADVERTISED_HOST_NAME=${KAFKA_IP}" \
	    -e "ZOOKEEPER_CONNECT=zookeeper-${RND}:2181" \
	    -e KAFKA_DELETE_TOPIC_ENABLE=true \
	    debezium/kafka:1.0

# kafka-start:
# 	-docker run -d --rm --name kafka-${RND} --network=${NETWORK} ${KAFKA_PORT} --network-alias=kafkaNet \
#       -e KAFKA_BROKER_ID=1 \
#       -e KAFKA_ZOOKEEPER_CONNECT=zooKeeperNet:2181 \
#       -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafkaNet:29092,PLAINTEXT_HOST://localhost:9092 \
#       -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
#       -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
#       -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
# 	    confluentinc/cp-kafka:5.4.0

kafka-stop:
	-docker stop kafka-${RND}


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
