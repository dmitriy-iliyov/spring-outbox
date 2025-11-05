#!/bin/bash
set -e

echo ">> creating Kafka topics ..."

kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --create --if-not-exists \
  --topic orders.created \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --create --if-not-exists \
  --topic orders.updated \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh \
  --bootstrap-server kafka:9092 \
  --create --if-not-exists \
  --topic orders.deleted \
  --partitions 3 \
  --replication-factor 1

echo ">> Kafka topics successfully created"