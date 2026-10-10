# API 错误响应格式

本文档描述了 AquaRush 项目中 API 错误响应的统一格式。

## 统一错误响应格式

### JSON 响应结构

```json
{
  "code": 403,
  "message": "权限不足，无法访问: GET /api/some-resource",
  "timestamp": "2025-01-06T10:30:00",
  "errorId": "a1b2c3d4",
  "correlationId": "GEN-e5f6g7h8"
}
```

### 字段说明

| 字段 | 类型 | 描述 |
|------|------|------|
| `code` | Integer | HTTP 状态码 |
| `message` | String | 错误描述信息（中文） |
| `timestamp` | LocalDateTime | 错误发生时间 |
| `errorId` | String | 唯一错误标识符（8位） |
| `correlationId` | String | 关联标识符（用于追踪请求链路） |

## 常见错误类型

### 403 Forbidden (权限不足)

当用户已认证但没有足够权限访问资源时返回：

```json
{
  "code": 403,
  "message": "权限不足，无法访问: GET /api/user/profile",
  "timestamp": "2025-01-06T10:30:00",
  "errorId": "a1b2c3d4",
  "correlationId": "GEN-e5f6g7h8"
}
```

### 401 Unauthorized (认证失败)

当用户未认证或认证信息无效时返回：

```json
{
  "code": 401,
  "message": "认证失败，请先登录: GET /api/user/profile",
  "timestamp": "2025-01-06T10:30:00",
  "errorId": "b2c3d4e5",
  "correlationId": "GEN-f6g7h8i9"
}
```

### 400 Bad Request (请求错误)

当请求参数无效时返回：

```json
{
  "code": 400,
  "message": "Validation failed: name: 姓名不能为空; email: 邮箱格式不正确",
  "timestamp": "2025-01-06T10:30:00",
  "errorId": "c3d4e5f6",
  "correlationId": "GEN-g7h8i9j0"
}
```

### 500 Internal Server Error (服务器错误)

当服务器内部发生错误时返回：

```json
{
  "code": 500,
  "message": "Internal server error occurred. Error ID: d4e5f6g7",
  "timestamp": "2025-01-06T10:30:00",
  "errorId": "d4e5f6g7",
  "correlationId": "GEN-h8i9j0k1"
}
```

## 错误处理机制

### Spring Security 集成

项目集成了多个层次的错误处理：

1. **CustomAccessDeniedHandler** - 处理 Spring Security 的访问拒绝异常
2. **CustomAuthenticationEntryPoint** - 处理认证失败异常
3. **GlobalExceptionHandler** - 处理应用程序中的各种异常

### 审计日志

所有安全相关的错误都会记录到审计日志中：

```
ACCESS_DENIED - CorrelationId: GEN-e5f6g7h8, ErrorId: a1b2c3d4, Method: GET, URI: /api/user/profile, User: user123, UserAgent: Mozilla/5.0..., RemoteAddr: 192.168.1.100
```

## 客户端处理建议

### 前端错误处理

```javascript
// 使用 Taro.request 的错误处理
try {
  const response = await Taro.request({
    url: `${apiBaseUrl}/api/user/profile`,
    method: 'GET',
    header: {
      'Authorization': `Bearer ${token}`
    }
  });

  // 处理成功响应
  console.log(response.data);
} catch (error) {
  // 处理错误响应
  if (error.statusCode === 403) {
    console.error('权限不足:', error.data.message);
    // 跳转到登录页或显示权限提示
  } else if (error.statusCode === 401) {
    console.error('认证失败:', error.data.message);
    // 重新登录或清除本地存储的认证信息
  } else {
    console.error('请求失败:', error.data.message);
    // 显示通用错误提示
  }
}
```

### GraphQL 错误处理

GraphQL 查询返回的错误格式略有不同：

```json
{
  "data": null,
  "errors": [
    {
      "message": "用户未认证，请先登录",
      "extensions": {
        "code": "UNAUTHORIZED",
        "errorId": "e5f6g7h8",
        "correlationId": "GEN-i9j0k1l2"
      }
    }
  ]
}
```

## 调试和监控

### 错误 ID 追踪

每个错误都有唯一的 `errorId`，可用于：

1. 在日志中查找详细错误信息
2. 用户反馈时快速定位问题
3. 监控系统统计错误类型

### 关联 ID 追踪

`correlationId` 用于追踪完整的请求链路，特别适用于：

1. 微服务之间的调用链追踪
2. 复杂业务流程的问题定位
3. 性能分析和优化

## 配置文件

相关的配置文件：

- `SecurityConfig.kt` - Spring Security 配置
- `CustomAccessDeniedHandler.kt` - 访问拒绝处理器
- `CustomAuthenticationEntryPoint.kt` - 认证入口点
- `GlobalExceptionHandler.kt` - 全局异常处理器
- `ApiResponse.kt` - 统一响应格式定义

## 示例测试

可以使用 curl 命令测试错误响应：

```bash
# 测试 401 认证失败
curl -i -X GET http://localhost:8080/api/user/profile

# 测试 403 权限不足
curl -i -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer invalid-token"

# 测试 400 请求错误
curl -i -X POST http://localhost:8080/api/user \
  -H "Content-Type: application/json" \
  -d '{"invalid": "data"}'
```