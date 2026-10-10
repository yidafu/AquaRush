# AquaRush 日志模块使用指南

> Moved out of `spec/tech/` (usage detail, not spec). Module source: `modules/aqua-foundation/.../logging/`.

## 概述

AquaRush 日志模块是一个全面的日志解决方案，提供结构化日志记录、业务上下文追踪、性能监控和安全审计功能。

## 核心功能

### 1. 关联ID追踪
- 自动为每个请求生成唯一的关联ID
- 支持从HTTP请求头中读取已有的关联ID
- 在整个请求生命周期中保持关联ID传递

### 2. 用户上下文管理
- 存储当前请求的用户信息
- 支持用户ID、用户名、角色等信息
- 线程安全的上下文管理

### 3. 结构化日志记录
- JSON格式的日志输出
- 自动添加时间戳、关联ID、用户信息
- 支持自定义字段和元数据

### 4. 业务日志记录
- 针对不同业务模块的专用日志格式
- 支持订单、支付、配送、用户等业务领域
- 内置业务状态和结果分类

### 5. 性能监控
- 自动记录方法执行时间
- 支持性能阈值警告
- 分类性能指标

### 6. 安全审计
- 记录安全相关事件
- 支持不同严重级别的安全日志
- 完整的审计跟踪

## 使用方法

### 1. 基本日志记录

```kotlin
import org.slf4j.LoggerFactory

class MyService {
    private val logger = LoggerFactory.getLogger(MyService::class.java)

    fun doSomething() {
        logger.info("操作完成")
    }
}
```

### 2. 业务日志记录

```kotlin
import dev.yidafu.aqua.logging.util.logBusiness

class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun processOrder(orderId: String) {
        logger.logBusiness(
            operation = "PROCESS_ORDER",
            module = "ORDER",
            result = "SUCCESS",
            message = "成功处理订单 $orderId",
            additionalData = mapOf(
                "orderId" to orderId,
                "amount" to 100.0
            )
        )
    }
}
```

### 3. BizLogger(落库业务日志)

后端业务操作需要**写入 business_logs 表**(供后台「业务日志」页查询)时,注入 `BizLogger`
(`dev.yidafu.aqua.logging.util.BizLogger`),双写数据库与控制台:

```kotlin
import dev.yidafu.aqua.logging.util.BizLogger

class AdminReconciliationMutationResolver(
    private val bizLogger: BizLogger,
) {
    fun confirmReconciliation(id: Long) {
        // 业务逻辑...
        bizLogger.logOperation(
            operation = "CONFIRM_RECONCILIATION",
            module = "RECONCILIATION",
            result = "SUCCESS",           // 非 SUCCESS 记 ERROR
            target = "reconciliation:$id",
            additionalData = mapOf("id" to id),
        )
    }
}
```

区分:`logBusiness` 只输出到日志文件;`BizLogger.logOperation` 落库 + 输出。前端用户行为上报用
`UserActionLogger`(同包)。

### 4. 性能监控

```kotlin
import dev.yidafu.aqua.logging.util.logPerformance

class DataProcessor {
    private val logger = LoggerFactory.getLogger(DataProcessor::class.java)

    fun processData(dataSize: Int) {
        val startTime = System.currentTimeMillis()

        // 执行处理逻辑

        val duration = System.currentTimeMillis() - startTime
        logger.logPerformance(
            operation = "DATA_PROCESSING",
            durationMs = duration,
            additionalData = mapOf(
                "dataSize" to dataSize,
                "recordsPerSecond" to (dataSize * 1000 / duration).toInt()
            )
        )
    }
}
```

### 5. 安全事件记录

```kotlin
import dev.yidafu.aqua.logging.util.logSecurity

class AuthService {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun logLoginAttempt(username: String, ipAddress: String, success: Boolean) {
        val severity = if (success) "INFO" else "MEDIUM"
        logger.logSecurity(
            eventType = "LOGIN_ATTEMPT",
            severity = severity,
            message = "登录尝试: 用户 $username 从 $ipAddress",
            details = mapOf(
                "username" to username,
                "ipAddress" to ipAddress,
                "result" to if (success) "SUCCESS" else "FAILURE"
            )
        )
    }
}
```

### 6. 审计日志记录

```kotlin
import dev.yidafu.aqua.logging.util.logAudit

class UserService {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun updateUser(userId: String, changes: Map<String, Any>) {
        // 执行更新逻辑

        logger.logAudit(
            action = "UPDATE_USER",
            resource = "USER",
            result = "SUCCESS",
            message = "用户信息已更新",
            additionalData = mapOf(
                "userId" to userId,
                "changes" to changes.keys
            )
        )
    }
}
```

### 7. 结构化日志记录

```kotlin
import dev.yidafu.aqua.logging.util.LogLevel
import dev.yidafu.aqua.logging.util.structured

class CustomService {
    private val logger = LoggerFactory.getLogger(CustomService::class.java)

    fun logCustomEvent(eventType: String, data: Map<String, Any>) {
        logger.structured(
            level = LogLevel.INFO,
            message = "自定义事件: $eventType",
            eventType = eventType,
            data = data
        )
    }
}
```

### 8. 用户上下文设置

```kotlin
import dev.yidafu.aqua.logging.context.UserContextHolder
import dev.yidafu.aqua.logging.context.UserContext

class AuthFilter : Filter {
    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        try {
            // 获取用户信息并设置上下文
            val userContext = UserContext(
                userId = "user123",
                username = "john_doe",
                role = "CUSTOMER"
            )
            UserContextHolder.setUserContext(userContext)

            chain.doFilter(request, response)
        } finally {
            // 清理上下文
            UserContextHolder.clear()
        }
    }
}
```

### 9. 异常日志记录

```kotlin
import dev.yidafu.aqua.logging.util.logError

class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun handleError(orderId: String, exception: Exception) {
        logger.logError(
            message = "处理订单时发生错误: $orderId",
            exception = exception,
            additionalData = mapOf(
                "orderId" to orderId,
                "operation" to "PROCESS_ORDER"
            )
        )
    }
}
```

## 配置

### 1. application.yml 配置

```yaml
aqua:
  logging:
    enabled: true
    structured:
      enabled: true
      format: JSON  # JSON 或 PLAIN
      includeStackTrace: false
      maxLength: 8192
    correlation:
      enabled: true
      headerName: X-Correlation-ID
      generateIfMissing: true
      includeInResponse: true
    performance:
      enabled: true
      slowThresholdMs: 1000
      verySlowThresholdMs: 5000
      logSlowQueries: true
      trackMethodExecution: true
    audit:
      enabled: true
      logSecurityEvents: true
      logBusinessOperations: true
      logDataAccess: false
      retentionDays: 90
    async:
      enabled: true
      queueSize: 256
      discardingThreshold: 0
      includeCallerData: true
      workerThreads: 1
```

### 2. Logback 配置

日志模块提供了完整的 Logback 配置，支持：

- 多环境配置（开发、测试、生产）
- 异步日志记录
- 文件轮转和归档
- 不同类型的日志分离存储
- JSON 格式输出

## 日志输出示例

### 1. 结构化业务日志

```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "order.OrderService",
  "message": "业务日志: PROCESS_ORDER",
  "correlationId": "abc123def456",
  "userId": "user123",
  "username": "john_doe",
  "businessOperation": "PROCESS_ORDER",
  "businessModule": "ORDER",
  "businessResult": "SUCCESS",
  "orderId": "ORD-2024-001",
  "amount": 150.50,
  "service": "aqua-rush"
}
```

### 2. 性能日志

```json
{
  "@timestamp": "2024-01-15T10:31:20.456Z",
  "level": "WARN",
  "logger": "performance",
  "message": "性能监控: DATA_PROCESSING",
  "correlationId": "abc123def456",
  "operation": "DATA_PROCESSING",
  "durationMs": 2500,
  "success": true,
  "performanceCategory": "SLOW",
  "dataSize": 1000,
  "service": "aqua-rush"
}
```

### 3. 安全审计日志

```json
{
  "@timestamp": "2024-01-15T10:32:10.789Z",
  "level": "MEDIUM",
  "logger": "security",
  "message": "安全事件: LOGIN_ATTEMPT",
  "correlationId": "xyz789uvw012",
  "security": true,
  "securityEventType": "LOGIN_ATTEMPT",
  "securitySeverity": "MEDIUM",
  "securityDetails": {
    "username": "john_doe",
    "ipAddress": "192.168.1.100",
    "result": "FAILURE"
  },
  "service": "aqua-rush"
}
```

## 最佳实践

1. **使用结构化日志**：优先使用业务日志记录方法，而不是简单的字符串日志
2. **添加上下文信息**：在日志中包含相关的业务标识和上下文
3. **合理使用日志级别**：根据事件的重要性和频率选择合适的日志级别
4. **避免敏感信息**：不要在日志中记录密码、密钥等敏感信息
5. **性能考虑**：在高频操作中谨慎使用日志，考虑异步配置
6. **监控和告警**：基于错误日志和性能日志设置监控和告警

## 故障排除

### 1. 日志不输出
- 检查配置文件中的日志级别设置
- 确认依赖是否正确引入
- 验证自动配置是否生效

### 2. 关联ID丢失
- 确认 CorrelationFilter 是否正确配置
- 检查异步线程中的上下文传递
- 验证请求处理流程的完整性

### 3. 性能问题
- 调整异步日志的队列大小
- 优化日志输出的详细程度
- 考虑使用采样策略

## 扩展和定制

日志模块支持以下扩展：

1. **自定义格式化器**：实现新的日志格式化逻辑
2. **自定义拦截器**：添加额外的请求处理逻辑
3. **扩展日志字段**：根据业务需要添加特定字段
4. **集成第三方系统**：与日志聚合和分析系统集成

## 总结

AquaRush 日志模块提供了完整的日志解决方案，支持现代微服务架构的日志记录需求。通过合理使用这些功能，可以显著提升系统的可观察性和故障排查能力。