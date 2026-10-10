# AquaRush 事件传输(Transaction Outbox)

## 概述

AquaRush 是单体(双进程共享一个 PostgreSQL)应用,领域事件不依赖任何外部 MQ,
基于 **transactional outbox** 传输:发布即向共享表 `outbox_events` INSERT(与业务写同事务),
两服务各自轮询认领并分发。设计文档:`docs/compose/spec/outbox-messaging.md`。

## 核心语义

- **原子性**:outbox INSERT 与业务写在同一数据库事务;失败向上传播回滚业务事务
  (业务成功 ⟺ 事件落库)。发布入口:`SimplifiedEventPublishService`。
- **点对点**:`subscriber IS NULL` 的行任一服务 `FOR UPDATE SKIP LOCKED` 认领一次
  (order/payment/delivery/user/notifications-events 四业务队列 + 通知队列)。
- **广播**:WebSocket 推送(`broadcast-events`)按 `aqua.outbox.subscribers`(admin/client)
  各写一行,各服务只消费自己名下的行,推送到本服务在线会话。
- **延迟投递**:`PAYMENT_EXPIRY_CHECK` 写未来 `next_run_at`,到期才可被认领(R-ORD-04);
  丢失由 `PaymentExpiryScheduler` 兜底扫描覆盖。
- **失败处理**:handler 抛异常 → 指数退避重试(默认 1s×2ⁿ,封顶 60s,最多 8 次)→
  `DEAD` 状态保留 payload/error_message;健康指标 `dead > 0` 报 **DOWN**(告警出口,
  不建 redrive API)。handler 必须幂等(at-least-once)。
- **崩溃恢复**:进程被杀遗留的 `PROCESSING` 行超过可见性超时(5min)自动复位 `PENDING`。
- **清理**:`DONE` 行保留 7 天,每日 04:40 分批删除(500/批);`DEAD` 永不自动清理。

## 架构组件(`dev.yidafu.aqua.common.messaging`)

| 组件 | 职责 |
|---|---|
| `OutboxEventPublisher` | `EventPublisher` 实现:按 `EventDestinationRouting` 写 outbox 行;`publishToSubscribers` 广播扇出 |
| `OutboxEventRepository` | Exposed 仓储:insert / claim(SKIP LOCKED) / markDone / markFailed / recoverStuck / 清理 / stats |
| `OutboxPoller` | 专用单线程轮询(默认 300ms):认领 → 按 destination 分发 → DONE/退避/DEAD |
| `OutboxDestinationHandler` | 消费契约(destination 集合 + `handle(record)`,失败抛异常) |
| `DomainEventOutboxHandler` | 四个业务队列 → `EventProcessor` 按 `DomainEventType` 分发 |
| `MessagingHealthIndicator` | `/actuator/health`:积压明细 + DEAD → DOWN |
| `OutboxRetentionScheduler` | DONE 保留期清理 |

## 配置

```yaml
aqua:
  messaging:
    enabled: true          # 发布侧总开关(INSERT)
  outbox:
    enabled: true          # 轮询分发开关
    # 本服务订阅的 destination(实例身份自动取 aqua.mode)
    # admin: 全部含 payment-expiry-events;client: 除 payment-expiry 外
  mode: admin | client
```

调优项(`aqua.outbox.*`):`poll-interval-ms=300`、`batch-size=10`、`max-attempts=8`、
`initial-backoff-ms=1000`、`backoff-multiplier=2.0`、`max-backoff-ms=60000`、
`visibility-timeout-ms=300000`、`done-retention-days=7`、`subscribers=[admin,client]`。

## 使用示例

```kotlin
// 发布(在业务事务内调用;失败会回滚业务事务)
eventPublishService.publishOrderPaid(orderId, userId, amountCents)

// 延迟事件(R-ORD-04)
eventPublishService.publishPaymentExpiryCheck(orderId, userId, expireAt)

// 消费端:注册 destination handler
@Component
class MyHandler : OutboxDestinationHandler {
  override val destinations = setOf(EventQueueConst.ORDER_QUEUE_NAME)
  override fun handle(record: OutboxEventRecord) {
    val event = jacksonObjectMapper().readValue(record.payload, DomainEvent::class.java)
    // 幂等处理;失败抛异常 → outbox 退避重试
  }
}
```

## 监控

- `GET /actuator/health`:`pendingDue / processing / dead / oldestPendingAgeMs` 明细;
  `dead > 0` → `DOWN`(健康告警)。
- 关键日志:`aqua-outbox-poller` 线程的分发失败(含 attempts)与崩溃复位告警。

## 故障排查

- **积压上涨(pendingDue 高)**:检查轮询线程日志、handler 耗时、DB 慢查询。
- **行停在 DEAD**:查 `outbox_events.error_message`/`payload`,修复后人工改回
  `PENDING, retry_count=0, next_run_at=now()`(暂无 redrive API)。
- **行停在 PROCESSING**:等待可见性超时复位(5min),或检查持锁实例是否存活。
- **延迟事件未触发**:确认发布方 `aqua.messaging.enabled=true`、admin 服务
  `aqua.outbox.enabled=true` 且订阅 `payment-expiry-events`;兜底扫描会覆盖丢失场景。
