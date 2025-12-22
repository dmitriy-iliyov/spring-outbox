![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791?logo=postgresql)
![MySQL](https://img.shields.io/badge/MySQL-8.0.1-336791?logo=mysql)
![Oracle](https://img.shields.io/badge/Oracle-23-336791)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-231F20?logo=apachekafka)

## Overview
This library provides an implementation of the Transactional Outbox Pattern based on polling events from a relational database and publishing them to a message broker.

It is designed for event-driven and microservice architectures where reliable message delivery and transactional consistency between the database and the messaging system are required.

The library offers flexible configuration for processing different event types.
Each event type can be handled independently, and events are processed in parallel using a configurable thread pool, allowing predictable throughput and controlled resource usage.

Two delivery guarantees are supported:
- **At-least-once delivery**
- **Exactly-once delivery**, achieved through an idempotent consumer implementation provided by the library

This approach ensures reliable event publication without relying on database log-based CDC solutions, making it suitable for environments where simplicity, portability, and explicit control over event processing are preferred.

## Key Features
- Atomic event storage within business transactions
- Scheduled polling and publishing to message broker
- Configurable retry policies with backoff
- Dead Letter Queue support
- Automatic stuck event recovery
- Automatic cleanup of processed events
- Idempotent event consumption
- Automatic cleanup of consumed events

## Limitations
- **Horizontal scaling** - performance degradation may occur when reaching a certain number of instances due to `SKIP LOCKED`-based concurrent polling mechanism. The optimal number of instances depends on the number of event types and database load.
- **No ordering guarantees** - events are processed in parallel by type, with no guaranteed delivery order within or across event types.

## Infrastructure

### Supported Databases
- PostgreSQL 16+
- MySQL 8+
- Oracle Database 23+

### Message Brokers
- Apache Kafka 3.7+
- RabbitMQ 3.13+

### Cache Storage
Redis 7+ is required for:
- Consumer-side event deduplication
- Metrics collection

## Design

### Architecture Overview

The system architecture with full configuration looks as follows:

The event lifecycle follows these steps:
1. Event or batch of events is created
2. Scheduled procedure polls a batch of events by type
3. Attempt to publish the batch:
    - On success: batch is marked as processed
    - On failure: configured number of retries is performed for this event type
        - When retries are exhausted: event is atomically moved to DLQ
4. Event is placed in the message queue

The following flow applies only when using the idempotent consumer:
1. Consumer receives an event or batch of events
2. Consumption check is performed (all subsequent steps are atomic):
    - If event was already consumed: business action is not executed and no exceptions are thrown
        - For batch consumption: events are filtered and business operation executes only for unconsumed events
    - If event was not consumed: event is marked as consumed and business operation is executed

---

### Polling Mechanism

The polling mechanism is based on selecting events by type, sorted by creation date (or by retry date after the first failed delivery attempt). Events of different types can be processed in parallel using threads from a dedicated library-managed thread pool.

To enable parallel event processing across multiple application instances, the library uses `FOR UPDATE SKIP LOCKED`, which allows concurrent processing of event batches. This architectural decision has two drawbacks:
- Performance degrades as the number of instances increases due to longer search times for available event batches
- Event processing order cannot be guaranteed

---

### Delivery Semantics

The library expects the message queue to be configured by the developer. Therefore, achieving either delivery semantic is possible with proper configuration of message acknowledgment on the broker side.

#### At-Least-Once

This delivery semantic is ensured without consumer-side configuration. The publisher waits for successful acknowledgment from the queue confirming event receipt.

If the publisher crashes after polling but before sending to the queue, events become stuck in `IN_PROCESS` state. They will be detected by a dedicated worker and transitioned back to `PENDING` state without counting the failed delivery attempt.

#### Exactly-Once

This semantic is achievable only when using the idempotent consumer, which deduplicates messages based on event identifiers generated when the event is initially saved at the beginning of its lifecycle.

In reality, the term "Exactly-Once" is not entirely accurate and should be understood as "Effectively-Exactly-Once", since the library cannot guarantee deduplication within the broker itself.

---

### Publisher

#### Transactional Guarantees
The library ensures atomic storage of outbox events within the same database transaction as business entity modifications. This guarantees that either both the business change and the event are saved, or neither is saved, preventing inconsistencies between the database and message broker.

Events are saved in the same database transaction as business entities
Spring's @Transactional(propagation=MANDATORY) boundary ensures all-or-nothing commit
No event is published without the corresponding business change being persisted

Manual invocation: OutboxPublisher.publish()
Annotation-based: @OutboxPublish above method


#### Event State Machine
Events transition through the following states:
PENDING → IN_PROCESS → PROCESSED
↓
FAILED → DLQ (after max retries exhausted)
State transitions:

PENDING: Event created and waiting to be polled
IN_PROCESS: Event currently being processed by a publisher instance
PROCESSED: Event successfully sent to message broker
FAILED: Event failed after exhausting all retry attempts

Error handling:

Transient failures: Event remains in current state, retry scheduled with backoff
Permanent failures: Event moved to DLQ after max retries exhausted
Stuck events: Detected by recovery worker and transitioned back to PENDING

#### Retry Policy
Backoff strategies:

Exponential backoff (default): Delay increases exponentially with each retry

Formula: initial_delay * (multiplier ^ retry_attempt)
Example: 10s, 30s, 90s, 270s...


Fixed backoff: Disabled backoff results in fixed delay between retries

max-retries: Maximum number of retry attempts before moving to DLQ
backoff.delay: Initial delay before first retry
backoff.multiplier: Multiplier for exponential backoff calculation
backoff.enabled: Enable/disable exponential backoff

#### Stuck Event Recovery

#### Dead Letter Queue

#### Cleanup

---

### Consumer
The consumer side of the library provides only manual invocation through the following interface:
```java
public interface OutboxIdempotentConsumer {
    <T> void consume(T message, Runnable operation);
    <T> void consume(List<T> messages, Consumer<List<T>> operation);
}
```
#### Idempotent Processing
Idempotent processing is implemented through unique event identifiers that are stored in a dedicated table when consumed. Storage occurs atomically together with the business effect.

Event identifiers are passed via message headers to avoid polluting business events, for:
- RabbitMQ - no special requirements
- Apache Kafka - developers must accept `ConsumerRecord.class` to access headers

As mentioned earlier, when consuming a batch of events, already-consumed events are filtered out and only unconsumed events are passed to the lambda expression.

For improved performance, consumed event identifiers can be cached in a distributed cache. When using this feature, ensure that cleanup of successfully processed events occurs after cache cleanup to prevent data loss.
The library uses `CacheManager` from the application context. Cache configuration must be provided by the developer.

#### Cleanup
The automatic cleanup strategy for consumed events is identical to the publisher cleanup strategy described above.

## Observability
### Publisher
Metrics exposed:

Event counts by status (PENDING, IN_PROCESS, PROCESSED, FAILED)
Event counts by event type
DLQ event counts by status and type
Processing time metrics (configurable gauge cache TTLs)

### Consumer
Metrics exposed:

Total consumed events count
Consumed events by status
Cache hits and misses (when caching enabled)
Duplicate detection count


## Configuration
### Global

```yaml
outbox:
  thread-pool-size: 5  # min(available_processors, 5)
  tables:
    auto-create: true
```

- `thread-pool-size`: size of the thread pool used for parallel event processing by type
  - **Default**: `min(available_processors, 5)` 
  - **Description**: events of different types are processed in parallel using threads from this pool. Scale based on number of event types and system resources

- `auto-create`: automatically create outbox tables on application startup
  - **Default**:`true`
  - **Description**: when enabled, the library creates `outbox_events`, `outbox_dlq_events`, and `outbox_consumed_events` tables with appropriate indexes if they don't exist

### Publisher

#### Sender
```yaml
outbox:
  publisher:
    sender:
      type: kafka  # or rabbitmq
      bean-name: "customKafkaTemplate"  # optional
      emergency-timeout: 120s
```

- `type`: message broker type (**required**)
  - **Values**: [`kafka`, `rabbitmq`]
- `bean-name`: custom sender bean name (**optional**) 
  - **Description**: use when you have multiple sender beans in the context.
- `emergency-timeout`: maximum time to wait for send operation
  - **Default**: `120s` 
  - **Description**: timeout for message broker send operations. If exceeded, operation is cancelled and event marked `FAILED`.

---
#### Defaults & Events

The `defaults` section defines default values that apply to all events unless overridden in individual event configuration.
```yaml
outbox:
  publisher:
    defaults:
      batch-size: 50
      initial-delay: 300s
      fixed-delay: 2s
      max-retries: 3
      backoff:
        enabled: true
        delay: 10s
        multiplier: 3
```

- `batch-size`: number of events to poll and process per iteration
    - **Default**: `50`
    - **Description**: larger batches improve throughput but increase memory usage
- `initial-delay`: delay before first polling starts after application startup
    - **Default**: `300s`
    - **Description**: gives application time to fully initialize before starting event processing
- `fixed-delay`: interval between polling iterations
    - **Default**: `2s`
    - **Description**: lower values reduce latency but increase database load
- `max-retries`: maximum retry attempts before moving to DLQ
    - **Default**: `3`
    - **Description**: total retry attempts. After exhaustion, event moves to DLQ
- `backoff.enabled`: enable exponential backoff
    - **Default**: `true`
    - **Description**: when enabled, delay between retries increases exponentially
- `backoff.delay`: initial backoff delay
    - **Default**: `10s`
    - **Description**: base delay for exponential backoff calculation
- `backoff.multiplier`: exponential backoff multiplier
    - **Default**: `3`
    - **Description**: each retry delay = previous_delay * multiplier

Individual event configurations override defaults for specific event types.
```yaml
outbox:
  publisher:
    events:
      order-created:
        topic: "orders.created"
        batch-size: 100
        fixed-delay: 1s
        max-retries: 5
        backoff:
          delay: 5s
          multiplier: 2
      
      order-updated:
        topic: "orders.updated"
        batch-size: 50
        fixed-delay: 2s
        backoff:
          enabled: false  # Use fixed delay instead
      
      notification-sent:
        topic: "notifications"
        # Other params inherited from defaults
```

- `topic`: message broker topic/queue name (**required**)
    - **Description**: destination topic for this event type 

All other parameters same as `defaults` section, but override defaults for this specific event type

**Example with inheritance from `defaults`:**
```yaml
outbox:
  publisher:
    defaults:
      batch-size: 50
      fixed-delay: 2s
      max-retries: 3
    
    events:
      high-priority:
        topic: "priority.events"
        fixed-delay: 1s  # Override: faster polling
        # Inherits: batch-size=50, max-retries=3
      
      low-priority:
        topic: "low.priority"
        fixed-delay: 10s  # Override: slower polling
        backoff:
          enabled: false  # Override: disable backoff
        # Inherits: batch-size=50, max-retries=3
```
---

#### Stuck Event Recovery
```yaml
outbox:
  publisher:
    stuck-recovery:
      batch-size: 100
      max-batch-processing-time: 300s
      initial-delay: 300s
      fixed-delay: 1800s
```

- `batch-size`: number of stuck events to recover per iteration
    - **Default**: `100`
- `max-batch-processing-time`: time threshold for detecting stuck events
    - **Default**: `300s`
    - **Description**: events in `IN_PROCESS` status longer than this are considered stuck
- `initial-delay`: delay before first recovery run
    - **Default**: `300s` 
- `fixed-delay`: interval between recovery runs
    - **Default**: `1800s` 

---

#### Dead Letter Queue
```yaml
outbox:
  publisher:
    dlq:
      enabled: true
      batch-size: 100
      transfer-to-initial-delay: 300s
      transfer-to-fixed-delay: 900s
      transfer-from-initial-delay: 300s
      transfer-from-fixed-delay: 3600s
      metrics:
        gauge:
          enabled: true
          cache:
            ttls: [60s, 60s, 60s]
```

- `enabled`: enable DLQ functionality
    - **Default**: `false`
    - **Warning**: when disabled, failed events are not managed automatically and stay in `outbox_events` as `FAILED`
- `batch-size`: number of events to transfer per iteration
    - **Default**: `100`
- `transfer-to-initial-delay`: delay before first transfer **to** DLQ
    - **Default**: `300s`
- `transfer-to-fixed-delay`: interval between transfers **to** DLQ
    - **Default**: `900s`
- `transfer-from-initial-delay`: delay before first transfer **from** DLQ **to** `outbox_events`
    - **Default**: `300s` 
- `transfer-from-fixed-delay`: interval between transfers **from** DLQ **to** `outbox_events`
    - **Default**: `3600s`
- `metrics.gauge.enabled`: enable DLQ metrics collection
    - **Default**: `false`
- `metrics.gauge.cache.ttls`: cache TTL for gauge metrics
    - **Default**: `[60s, 60s, 60s]`

---

#### Cleanup
```yaml
outbox:
  publisher:
    clean-up:
      enabled: true
      batch-size: 100
      ttl: 1h
      initial-delay: 120s
      fixed-delay: 5s
```

- `enabled`: enable automatic cleanup of processed events
    - **Default**: `true`
    - **Warning**: when disabled, processed events will accumulate indefinitely
- `batch-size`: number of events to delete per iteration
    - **Default**: `100`
    - **Description**: controls transaction size.
- `ttl`: time-to-live for processed events
    - **Default**: `1h`
    - **Description**: events with `PROCESSED` status older than this are deleted
- `initial-delay`: delay before first cleanup run
    - **Default**: `120s`
- `fixed-delay`: interval between cleanup runs
    - **Default**: `5s`

---

#### Publisher Metrics
```yaml
outbox:
  publisher:
    metrics:
      gauge:
        enabled: true
        cache:
          ttls: [60s, 60s, 30s]
```

- `gauge.enabled`: enable gauge metrics collection
    - **Default**: `false`
- `gauge.cache.ttls`: cache TTL for different gauge metrics (3 values)
    - **Default**: `[60s, 60s, 60s]`
    - **Description**: TTL for caching metric values.

---

### Consumer

#### Cleanup
```yaml
outbox:
  consumer:
    clean-up:
      enabled: true
      batch-size: 100
      ttl: 24h
      initial-delay: 120s
      fixed-delay: 5s
```

All parameters same as Publisher cleanup with different defaults:
- `ttl`
  - **Default**: `1h`
  - **Description**: consumed events are typically kept longer than published events for audit purposes

---

#### Cache
The library uses Spring's `CacheManager` from application context.
```yaml
outbox:
  consumer:
    cache:
      enabled: true
      cache-name: "outbox:consumed"
```

- `enabled`: enable distributed caching of consumed event ids
    - **Default**: `false`
    - **Warning**: when disabled, idempotency check always hits database
- `cache-name`: name of the cache in CacheManager (**required** when enabled)
    - **Description**: must match cache name configured in your `CacheManager` bean

---

### Complete Configuration Examples
#### Full
```yaml
outbox:
  thread-pool-size: 5
  tables:
    auto-create: true
    
  publisher:
    sender:
      type: kafka
      emergency-timeout: 120s

    defaults:
      batch-size: 100
      initial-delay: 30s
      fixed-delay: 2s
      max-retries: 3
      backoff:
        enabled: true
        delay: 10s
        multiplier: 3
    
    events:
      order-created:
        topic: "orders.created"
        batch-size: 100
        fixed-delay: 1s
      order-updated:
        topic: "orders.updated"
        batch-size: 50
        fixed-delay: 1s
      order-deleted:
        topic: "orders.deleted"
        batch-size: 50
        fixed-delay: 10s
        backoff:
          enabled: false
    
    stuck-recovery:
      batch-size: 100
      max-batch-processing-time: 300s
      initial-delay: 120s
      fixed-delay: 1800s
    
    clean-up:
      enabled: true
      batch-size: 100
      ttl: 1h
      initial-delay: 120s
      fixed-delay: 5s
    
    dlq:
      enabled: true
      batch-size: 100
      transfer-to-initial-delay: 120s
      transfer-to-fixed-delay: 900s
      transfer-from-initial-delay: 120s
      transfer-from-fixed-delay: 3600s
      metrics:
        gauge:
          enabled: true
          cache:
            ttls: [60s, 60s, 60s]
    
    metrics:
      gauge:
        enabled: true
        cache:
          ttls: [60s, 60s, 30s]

    consumer:
      enabled: true
      clean-up:
        enabled: true
        batch-size: 100
        ttl: 24h
        initial-delay: 120s
        fixed-delay: 120s
      cache:
        enabled: true
        cache-name: "outbox:consumed"
```

#### Consumer-Only
```yaml
outbox:
  thread-pool-size: 2
  tables:
    auto-create: true

  publisher:
    enabled: false

  consumer:
    enabled: true
    clean-up:
      enabled: true
      batch-size: 100
      ttl: 24h
      initial-delay: 120s
      fixed-delay: 120s
    cache:
      enabled: true
      cache-name: "outbox:consumed"
```

#### Minimal
```yaml
outbox:
  publisher:
    sender:
      type: kafka
    events:
      my-event:
        topic: "my.topic"
```
All other values will use defaults.

## Getting started
## Recommendation