
# Rabbit Kafka Bridge

This project provides Flink Sink / Sources for Kafka and RabbitMQ

The sources and sinks are designed to be generic and always
write and read data as ByteArray. You use Map and FlapMap functions
to convert from ByteArray to whatever format you want the data
to be in.

See Integration tests and Makefile for usage examples

Note: Integration tests may require services such as RabbitMQ and Kafka to be running.
These external services can be started with the examples provided in the Makefile





