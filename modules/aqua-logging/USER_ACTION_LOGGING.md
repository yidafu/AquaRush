# 用户操作日志增强模块

本文档介绍了 AquaRush 项目中新增的用户操作日志功能，该功能允许系统记录和监控用户的各种操作行为，包括页面访问、点击、表单提交、搜索等。

## 功能概述

### 核心特性

1. **多类型用户操作记录**
   - 页面访问 (PAGE_VIEW)
   - 点击事件 (CLICK)
   - 拖拽操作 (DRAG)
   - 输入事件 (INPUT)
   - 表单提交 (FORM_SUBMIT)
   - 搜索操作 (SEARCH)
   - 分享操作 (SHARE)
   - 滚动操作 (SCROLL)
   - 文件操作 (FILE_OPERATION)
   - 自定义操作

2. **异步批量处理**
   - 高性能异步处理机制
   - 批量处理减少I/O开销
   - 可配置的批处理大小和刷新间隔
   - 降级机制确保可靠性

3. **灵活的配置选项**
   - 可独立控制各种操作类型的记录
   - 敏感数据自动脱敏
   - 冷热数据分离存储
   - 日志保留策略

4. **RESTful API接口**
   - 单个用户操作记录接口
   - 批量用户操作记录接口
   - 完整的请求验证和错误处理

## 配置说明

### application.yml 配置示例

```yaml
aqua:
  logging:
    user-action:
      enabled: true                    # 是否启用用户操作日志
      log-page-views: true            # 是否记录页面访问
      log-clicks: true               # 是否记录点击事件
      log-inputs: false              # 是否记录输入事件（默认关闭以减少日志量）
      log-backend-ops: true          # 是否记录后台操作
      log-scrolls: false             # 是否记录滚动事件（默认关闭）
      log-form-submits: true         # 是否记录表单提交
      log-file-operations: true      # 是否记录文件操作
      log-searches: true             # 是否记录搜索操作
      log-shares: true               # 是否记录分享操作
      retention-days: 30            # 日志保留天数
      batch-size: 100               # 批处理大小
      flush-interval: 5000          # 刷新间隔（毫秒）
      sanitize-sensitive-data: true  # 是否对敏感数据进行脱敏
      max-input-length: 200          # 输入内容最大记录长度
      excluded-elements: []           # 排除的元素ID列表
      included-operations: []        # 包含的操作类型列表（空表示全部）
      async-processing: true         # 是否启用异步处理
      enable-hot-cold-separation: true # 是否启用冷热数据分离
      hot-data-retention-days: 180   # 热数据保留天数
```

## API 接口

### 1. 记录单个用户操作

**接口地址**: `POST /api/user-actions/log`

**请求体**:
```json
{
  "userId": "用户ID（可选）",
  "username": "用户名（可选）",
  "actionType": "PAGE_VIEW",
  "target": "页面URL或元素ID",
  "coordinates": {
    "screenX": 100,
    "screenY": 200,
    "pageX": 50,
    "pageY": 150
  },
  "properties": {
    "pageTitle": "页面标题",
    "referrer": "来源页面",
    "elementType": "button",
    "elementText": "按钮文本"
  },
  "timestamp": 1234567890123
}
```

**响应**:
```json
{
  "code": 200,
  "message": "User action logged successfully",
  "data": null
}
```

### 2. 批量记录用户操作

**接口地址**: `POST /api/user-actions/batch`

**请求体**:
```json
{
  "actions": [
    {
      "userId": "用户ID",
      "username": "用户名",
      "actionType": "PAGE_VIEW",
      "target": "/home",
      "properties": {
        "pageTitle": "首页"
      },
      "timestamp": 1234567890123
    },
    {
      "actionType": "CLICK",
      "target": "submit-button",
      "coordinates": {
        "screenX": 100,
        "screenY": 200,
        "pageX": 50,
        "pageY": 150
      },
      "properties": {
        "elementType": "button",
        "elementText": "提交"
      },
      "timestamp": 1234567890124
    }
  ]
}
```

## 支持的操作类型

### 1. 页面访问 (PAGE_VIEW)

记录用户访问页面的信息。

**必要字段**:
- `target`: 页面URL

**可选字段**:
- `pageTitle`: 页面标题
- `referrer`: 来源页面

**示例**:
```json
{
  "actionType": "PAGE_VIEW",
  "target": "/products",
  "properties": {
    "pageTitle": "产品列表",
    "referrer": "/home"
  }
}
```

### 2. 点击事件 (CLICK)

记录用户点击元素的信息。

**必要字段**:
- `target`: 元素ID或选择器
- `coordinates`: 点击坐标

**可选字段**:
- `elementType`: 元素类型
- `elementText`: 元素文本

**示例**:
```json
{
  "actionType": "CLICK",
  "target": "submit-button",
  "coordinates": {
    "screenX": 100,
    "screenY": 200,
    "pageX": 50,
    "pageY": 150
  },
  "properties": {
    "elementType": "button",
    "elementText": "提交订单"
  }
}
```

### 3. 表单提交 (FORM_SUBMIT)

记录用户提交表单的信息。

**必要字段**:
- `target`: 表单ID

**可选字段**:
- `formType`: 表单类型
- `success`: 是否成功
- `fields`: 表单字段列表

**示例**:
```json
{
  "actionType": "FORM_SUBMIT",
  "target": "registration-form",
  "properties": {
    "formType": "user-registration",
    "success": true,
    "fields": ["username", "email", "password"]
  }
}
```

### 4. 搜索操作 (SEARCH)

记录用户搜索行为。

**必要字段**:
- `target`: 搜索类型标识

**可选字段**:
- `searchQuery`: 搜索关键词
- `resultCount`: 结果数量
- `searchTime`: 搜索耗时（毫秒）

**示例**:
```json
{
  "actionType": "SEARCH",
  "target": "product-search",
  "properties": {
    "searchQuery": "矿泉水",
    "resultCount": 15,
    "searchTime": 250
  }
}
```

## 日志输出格式

用户操作日志以结构化JSON格式输出，包含以下字段：

```json
{
  "@timestamp": "2023-05-01T12:00:00.000+08:00",
  "level": "INFO",
  "logger": "user-action",
  "eventType": "USER_ACTION",
  "actionType": "PAGE_VIEW",
  "target": "/home",
  "correlationId": "abc123-def456",
  "userId": "user123",
  "username": "张三",
  "userRole": "CUSTOMER",
  "tenantId": "tenant1",
  "pageTitle": "首页",
  "referrer": "/login",
  "userAgent": "Mozilla/5.0...",
  "clientIp": "192.168.1.100",
  "service": "aqua-rush",
  "version": "1.0.0"
}
```

## 使用示例

### 后端代码中使用

```kotlin
@Service
class OrderService(
  private val userActionLogger: UserActionLogger
) {

  fun createOrder(orderRequest: CreateOrderRequest): Order {
    // 业务逻辑...

    // 记录订单创建操作
    userActionLogger.logBackendOperation(
      operation = "CREATE_ORDER",
      module = "ORDER",
      result = "SUCCESS",
      target = orderRequest.productId,
      additionalData = mapOf(
        "orderId" to order.id,
        "amount" to order.amount,
        "customerId" to order.userId
      )
    )

    return order
  }
}
```

### 前端集成示例

```javascript
// 前端埋点SDK示例
class UserTracker {
  static trackPageView(pagePath, pageTitle) {
    fetch('/api/user-actions/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        actionType: 'PAGE_VIEW',
        target: pagePath,
        properties: { pageTitle }
      })
    });
  }

  static trackClick(elementId, elementText, coordinates) {
    fetch('/api/user-actions/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        actionType: 'CLICK',
        target: elementId,
        coordinates: coordinates,
        properties: {
          elementType: 'button',
          elementText
        }
      })
    });
  }
}

// 使用示例
UserTracker.trackPageView('/products', '产品列表');
UserTracker.trackClick('add-to-cart', '加入购物车', {
  screenX: 100, screenY: 200,
  pageX: 50, pageY: 150
});
```

## 性能优化

### 1. 异步处理

- 用户操作先进入内存队列
- 定期批量处理，减少数据库写入
- 失败时自动降级到同步处理

### 2. 批量处理优化

- 可配置批处理大小（默认100）
- 可配置刷新间隔（默认5秒）
- 队列满时自动触发批量处理

### 3. 数据存储优化

- 热数据存储在数据库中（默认180天）
- 冷数据转储到文件系统
- 自动清理过期数据

## 安全和隐私

### 敏感数据脱敏

系统自动对以下类型的敏感数据进行脱敏处理：

- **密码**: 替换为 `***`
- **邮箱**: 只显示前2位字符，如 `ab***@example.com`
- **手机号**: 只显示前3位和后4位，如 `138****1234`
- **银行卡号**: 只显示前4位和后4位，如 `6222****1234`
- **长文本**: 超过200字符自动截断

### 隐私保护

- 最小化数据收集原则
- 可配置排除特定元素
- 用户操作日志独立存储
- 支持数据删除和导出

## 监控和告警

### 关键指标

- 用户操作日志处理速度
- 队列积压情况
- 异步处理成功率
- 存储空间使用情况

### 日志文件

- **位置**: `./logs/aqua-rush-user-action.log`
- **轮转**: 按日期轮转，支持压缩
- **保留**: 默认30天
- **大小限制**: 单文件最大100MB

## 故障处理

### 异步处理失败

当异步处理失败时，系统会自动降级到同步处理，确保用户操作日志不丢失。

### 队列积压

当队列积压超过阈值时：
- 增加处理频率
- 自动触发批量处理
- 记录告警日志

### 存储空间不足

当存储空间不足时：
- 自动清理过期日志
- 压缩历史日志文件
- 记录错误并继续处理

## 扩展开发

### 添加新的操作类型

1. 在 `UserActionEventService.processUserActionSync()` 中添加新的操作类型处理
2. 在 `UserActionLogFormatter` 中添加对应的格式化方法（可选）
3. 更新API文档和示例

### 自定义处理器

可以通过实现 `UserActionProcessor` 接口来自定义用户操作的处理逻辑：

```kotlin
interface UserActionProcessor {
  fun canProcess(actionType: String): Boolean
  fun process(request: UserActionLogRequest): Boolean
}
```

## 最佳实践

1. **合理配置**: 根据业务需要选择合适的操作类型进行记录
2. **性能监控**: 定期监控异步处理的性能指标
3. **存储管理**: 合理设置日志保留策略，避免存储空间不足
4. **隐私保护**: 确保敏感数据得到适当处理
5. **测试验证**: 在生产环境部署前充分测试各种场景

## 故障排除

### 常见问题

1. **用户操作日志未记录**
   - 检查配置是否启用
   - 确认日志级别设置
   - 查看错误日志

2. **异步处理延迟**
   - 检查队列大小配置
   - 确认处理线程状态
   - 监控系统资源使用

3. **日志文件过大**
   - 调整轮转策略
   - 减少记录的操作类型
   - 启用数据压缩

### 日志级别

- `DEBUG`: 详细的处理过程信息
- `INFO`: 正常的操作记录
- `WARN`: 性能警告和处理异常
- `ERROR`: 严重错误和系统故障