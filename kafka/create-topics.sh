#!/bin/bash
# SkyWays Airlines — Kafka topic initialisation
# Runs once inside the kafka container after the broker is ready.

set -e

BOOTSTRAP="kafka:9092"
PARTITIONS=6
REPLICATION=1   # 1 replica in docker-compose; set to 3 in Kubernetes

wait_for_kafka() {
  echo "[kafka-init] Waiting for Kafka broker at ${BOOTSTRAP}..."
  until kafka-broker-api-versions --bootstrap-server "${BOOTSTRAP}" > /dev/null 2>&1; do
    sleep 2
  done
  echo "[kafka-init] Kafka is ready."
}

create_topic() {
  local name="$1"
  local parts="${2:-$PARTITIONS}"
  local rep="${3:-$REPLICATION}"
  kafka-topics --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${name}" \
    --partitions "${parts}" \
    --replication-factor "${rep}" \
    --config retention.ms=604800000
  echo "[kafka-init] Ready: ${name}"
}

wait_for_kafka

# Booking saga
create_topic booking-initiated
create_topic booking-confirmed
create_topic booking-cancelled

# Seat reservation
create_topic seat-reservation-requested
create_topic seat-reservation-confirmed
create_topic seat-reservation-failed

# Payment
create_topic payment-initiation-requested
create_topic payment-processed
create_topic payment-failed

# Notifications
create_topic notification-requested   3
create_topic notification-dlq         1

# User / Flight events
create_topic user-events              3
create_topic flight-status-changed    3

# Saga coordination
create_topic saga-events             12

echo "[kafka-init] All topics created successfully."