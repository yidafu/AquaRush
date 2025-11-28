# AquaRush 本地消息队列系统

## 概述

AquaRush本地消息队列系统是基于嵌入式ActiveMQ Artemis实现的高性能消息队列机制，避免了外部消息队列的复杂性，同时提供了优秀的性能和可靠性保障。

## 核心特性

### 1. 持久化事件处理模式
- **ActiveMQ Artemis**: 基于嵌入式ActiveMQ Artemis提供可靠的消息队列
- **消息持久化**: 所有消息都持久化存储，确保应用重启后不丢失
- **零数据丢失**: 通过持久化机制确保消息不丢失

### 2. 高性能
- **100ms延迟**: 内存队列处理延迟低至100毫秒
- **批量处理**: 支持批量事件处理提高吞吐量
- **并发安全**: 使用线程安全的数据结构

### 3. 可靠性保障
- **事务一致性**: 事件发布与业务操作在同一事务中
- **重试机制**: 指数退避重试策略（1s, 5s, 15s, 1min, 5min）
- **死信队列**: 失败事件发送到死信队列
- **自动清理**: 30天后自动清理已完成事件

## 架构组件

### EventPublisher 接口
```kotlin
interface EventPublisher {
    suspend fun publish(event: DomainEvent): Boolean
    fun publishSync(event: DomainEvent): Boolean
    fun getName(): String
    fun isAvailable(): Boolean
    fun getType(): EventTypePublisherType
}
```

### 发布器类型
1. **ArtemisEventPublisher**: 基于ActiveMQ Artemis的发布器，提供高性能和持久化保障

### 事件处理器
1. **MessageConsumer**: 处理ActiveMQ Artemis队列中的事件

## 配置说明

### application.yml 配置
```yaml
aqua:
  messaging:
    enabled: true
    strategy: artemis # artemis, hybrid, outbox-only, memory-only
    artemis:
      retry:
        max-attempts: 3
        initial-interval: 1000
        multiplier: 2.0
        max-interval: 30000
      pool:
        enabled: true
        max-connections: 10
        max-sessions-per-connection: 50

spring:
  artemis:
    mode: embedded
    embedded:
      enabled: true
      persistent: true
      data-directory: ./data/artemis
      queues:
        - order-events
        - payment-events
        - delivery-events
        - user-events
      topics:
        - broadcast-events
```

### 策略选择
- **artemis**: 推荐模式，基于ActiveMQ Artemis提供高性能和持久化保障
- **hybrid**: 混合模式，优先使用ActiveMQ Artemis，失败时回退到数据库Outbox
- **outbox-only**: 仅使用数据库模式，适用于可靠性优先的场景
- **memory-only**: 仅使用内存队列，适用于高性能场景

## 使用示例

### 1. 使用EventPublishService
```kotlin
@Service
class MyService(
    private val eventPublishService: EventPublishService
) {

    fun processOrder() {
        // 发布订单支付事件
        val success = eventPublishService.publishOrderPaid(
            orderId = DefaultIdGenerator().generate(),
            userId = DefaultIdGenerator().generate(),
            productId = DefaultIdGenerator().generate(),
            amount = BigDecimal("100.00")
        )

        if (success) {
            println("事件发布成功")
        }
    }
}
```

### 2. 直接使用EventPublisher
```kotlin
@Component
class MyComponent(
    private val eventPublisher: EventPublisher
) {

    fun handleSomething() {
        val eventData = mapOf(
            "userId" to userId,
            "action" to "created",
            "timestamp" to System.currentTimeMillis()
        )

        eventPublisher.publishSync(DomainEvent(
            eventType = "USER_ACTION",
            payload = objectMapper.writeValueAsString(eventData)
        ))
    }
}
```

### 3. 批量发布事件
```kotlin
fun publishBatchEvents() {
    val events = listOf(
        Triple("ORDER_PAID", orderId1, orderData1),
        Triple("ORDER_CREATED", orderId2, orderData2)
    )

    val results = eventPublishService.publishDomainEventsBatch(events)
    val successCount = results.count { it }
    println("批量发布成功: $successCount/${events.size}")
}
```

## 监控和健康检查

### 1. 健康检查端点
- **URL**: `/actuator/health`
- **指标**: 发布器状态、队列大小、处理器可用性
- **状态**: UP, WARNING, DEGRADED, DOWN

### 2. 性能指标
- **事件发布计数**: `event.published.total`
- **事件处理计数**: `event.processed.total`
- **队列大小**: `event.queue.size`
- **处理延迟**: `event.processing.duration.mean`
- **成功率**: `event.success_rate`

### 3. 详细指标
访问 `/actuator/metrics` 端点获取详细指标：
```json
{
  "name": "event.published",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1500
    }
  ],
  "availableTags": [
    {
      "tag": "publisher",
      "values": ["ArtemisEventPublisher"]
    },
    {
      "tag": "event_type",
      "values": ["ORDER_PAID", "ORDER_CANCELLED"]
    }
  ]
}
```

## 性能优化建议

### 1. 队列容量管理
- 监控队列大小，超过80%时发出告警
- 根据业务量调整队列配置
- 使用批量处理减少队列访问频率

### 2. 批量处理优化
- 调整批量处理参数找到最佳值
- 监控批量处理时间和成功率
- 避免过大的批量导致的延迟

### 3. 连接池配置
- 根据并发量调整连接池大小
- 监控连接池使用情况
- 优化会话管理

## 故障排查

### 1. 常见问题
- **队列积压**: 检查消费者处理能力和事件生产速率
- **处理延迟**: 检查事件处理器性能和连接池配置
- **事件丢失**: 检查持久化配置和磁盘空间

### 2. 日志级别
- **DEBUG**: 详细的事件处理流程
- **INFO**: 重要的事件状态变更
- **WARN**: 重试和降级处理
- **ERROR**: 处理失败和系统错误

### 3. 监控告警
- 队列使用率超过80%
- 事件处理失败率超过5%
- 平均处理延迟超过1秒
- 发布器不可用超过30秒

## 扩展和自定义

### 1. 自定义事件类型
```kotlin
object CustomEventType {
    const val USER_REGISTERED = "USER_REGISTERED"
    const val PRODUCT_UPDATED = "PRODUCT_UPDATED"
}
```

### 2. 自定义事件处理器
```kotlin
@Component
class CustomEventHandler {
    fun handle(event: DomainEvent) {
        when (event.eventType) {
            CustomEventType.USER_REGISTERED -> handleUserRegistered(event)
            CustomEventType.PRODUCT_UPDATED -> handleProductUpdated(event)
        }
    }
}
```

### 3. 自定义发布器
```kotlin
@Component
class CustomEventPublisher : EventPublisher {
    override fun publish(event: DomainEvent): Boolean {
        // 自定义发布逻辑
        return true
    }

    override fun getName(): String = "CustomPublisher"
    override fun isAvailable(): Boolean = true
    override fun getType(): EventTypePublisherType = EventTypePublisherType.MEMORY_QUEUE
}
```

## 最佳实践

1. **使用ActiveMQ Artemis模式**: 在生产环境中推荐使用 `artemis` 策略
2. **监控关键指标**: 设置队列大小、处理延迟、成功率的监控告警
3. **优雅降级**: 确保在Artemis不可用时能自动回退到Outbox
4. **批量处理**: 对于高并发场景，优先使用批量发布接口
5. **事务边界**: 事件发布应该在业务事务内完成
6. **错误处理**: 实现完善的错误处理和重试机制
7. **性能测试**: 在部署前进行充分的性能和压力测试

## 与外部消息队列对比

| 特性 | 本地消息队列 | RabbitMQ/Kafka |
|------|-------------|-------------|
| 部署复杂度 | 低 | 高 |
| 运维成本 | 低 | 高 |
| 数据可靠性 | 高（事务保证） | 高（持久化配置） |
| 实时性能 | 优秀（100ms） | 优秀（<50ms） |
| 吞吐量 | 良好（>1000/s） | 优秀（>5000/s） |
| 扩展性 | 单机限制 | 水平扩展 |
| 监控复杂度 | 简单 | 中等 |
| 故障排查 | 简单 | 复杂 |

对于中小型应用，本地消息队列在保持足够性能的同时，大大降低了架构复杂度和运维成本。