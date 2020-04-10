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

default:
	@echo ${NETWORK}
	@echo ${CURDIR}
	@echo ${USER}
	@echo ${USER_ID}
	@echo ${RND}

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
# IP: 172.18.0.6
#________________________________________________________________________________
zookeeper-start:
	-docker run -d --rm --name zookeeper-${RND} --network=${NETWORK} --network-alias=zooKeeperNet ${ZOOKEEPER_PORT} \
	    -e "ZOO_4LW_COMMANDS_WHITELIST=*" \
	    zookeeper:3.5.5

zookeeper-stop:
	-docker stop zookeeper-${RND}


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
# Kafka
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
