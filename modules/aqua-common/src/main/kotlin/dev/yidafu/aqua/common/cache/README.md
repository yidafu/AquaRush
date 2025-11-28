# AquaRush MapDB 缓存系统

这是一个基于 MapDB 的缓存抽象层实现，提供了高性能、可配置的缓存解决方案。

## 功能特性

- 🚀 **高性能**: 基于 MapDB 的内存数据库，支持持久化
- 🔄 **多命名空间**: 支持多个独立的缓存命名空间
- ⏰ **TTL 支持**: 支持键级别的过期时间设置
- 📊 **统计监控**: 内置缓存统计和监控功能
- 🔧 **灵活配置**: 支持通过 application.yml 进行配置
- 🏗️ **抽象设计**: 良好的抽象层，方便后续替换实现
- 📝 **注解支持**: 提供声明式缓存注解
- 🧵 **异步操作**: 支持异步缓存操作

## 快速开始

### 1. 添加依赖

依赖已添加到 `common-module`，其他模块可以直接使用：

```kotlin
// 在 build.gradle.kts 中
implementation(project(":modules:aqua-common"))
```

### 2. 配置缓存

在 `application.yml` 中添加缓存配置：

```yaml
aqua:
  cache:
    enabled: true
    default:
      ttl: PT30M
      max-size: 10000
      eviction-policy: LRU
    namespaces:
      products:
        ttl: PT1H
        max-size: 20000
```

### 3. 使用缓存

#### 编程式缓存

```kotlin
@Service
class ProductService(
    private val cacheTemplate: CacheTemplate,
    private val productRepository: ProductRepository
) {

    fun getProductById(productId: UUID): Product? {
        return cacheTemplate.getOrPut("products", "product:$productId", Product::class.java, {
            // 缓存未命中时从数据库加载
            productRepository.findById(productId).orElse(null)
        }, Duration.ofMinutes(30))
    }

    fun createProduct(product: Product): Product {
        val newProduct = productRepository.save(product)

        // 清除相关缓存
        cacheTemplate.remove("products", "product:${newProduct.id}")
        cacheTemplate.clear("product-list")

        return newProduct
    }
}
```

#### 注解式缓存（待实现 AOP 切面）

```kotlin
@Service
class ProductService {

    @Cacheable(key = "#productId", namespace = "products", ttl = "30m")
    fun getProductById(productId: UUID): Product? {
        return productRepository.findById(productId).orElse(null)
    }

    @CacheEvict(key = "#product.id", namespace = "products")
    fun updateProduct(product: Product): Product {
        return productRepository.save(product)
    }
}
```

## 核心组件

### CacheManager

缓存管理器接口，提供基本的缓存操作：

```kotlin
interface CacheManager {
    fun <T> put(key: String, value: T, ttl: Duration? = null)
    fun <T> get(key: String, clazz: Class<T>): T?
    fun <T> getOrPut(key: String, clazz: Class<T>, supplier: () -> T, ttl: Duration? = null): T
    fun remove(key: String): Boolean
    fun clear()
    fun size(): Long
    // ... 更多方法
}
```

### CacheNamespace

命名空间管理器，支持多个独立缓存：

```kotlin
interface CacheNamespace {
    fun getCache(namespace: String): CacheManager
    fun createNamespace(namespace: String, config: CacheConfig): CacheManager
    fun clearNamespace(namespace: String)
    fun deleteNamespace(namespace: String): Boolean
}
```

### CacheTemplate

简化的缓存操作模板：

```kotlin
@Component
class CacheTemplate(private val cacheNamespace: CacheNamespace) {
    fun <T> put(key: String, value: T, ttl: Duration? = null)
    fun <T> get(key: String, clazz: Class<T>): T?
    fun <T> getOrPut(key: String, clazz: Class<T>, supplier: () -> T, ttl: Duration? = null): T
    fun <T> put(namespace: String, key: String, value: T, ttl: Duration? = null)
    fun <T> get(namespace: String, key: String, clazz: Class<T>): T?
    // ... 更多方法
}
```

## 配置选项

### 基本配置

```yaml
aqua:
  cache:
    enabled: true                    # 是否启用缓存
    default:
      ttl: PT30M                    # 默认TTL
      max-size: 10000               # 最大缓存条目数
      eviction-policy: LRU          # 淘汰策略
      enable-expiration: true       # 是否启用过期清理
      enable-compression: false     # 是否启用压缩
      enable-async: true            # 是否启用异步操作
```

### 命名空间配置

```yaml
aqua:
  cache:
    namespaces:
      user-sessions:
        ttl: PT2H
        max-size: 50000
        eviction-policy: LRU
      products:
        ttl: PT1H
        max-size: 20000
        eviction-policy: LFU
```

### 持久化配置

```yaml
aqua:
  cache:
    global:
      persistence:
        enabled: true               # 是否启用持久化
        file-path: "./cache"        # 持久化文件路径
        async: true                 # 是否异步持久化
        flush-interval: PT5M        # 持久化间隔
        force-flush-on-shutdown: true # 关闭时强制持久化
```

### 统计配置

```yaml
aqua:
  cache:
    global:
      stats:
        enabled: true               # 是否启用统计
        record-hit-rate: true       # 是否记录命中率
        report-interval: 60000      # 统计报告间隔
```

## 淘汰策略

- `LRU`: 最近最少使用
- `LRU_2Q`: 最近最少使用（2-Q算法）
- `LFU`: 最少使用频率
- `FIFO`: 先进先出
- `WEAK`: 弱引用
- `SOFT`: 软引用
- `NONE`: 不淘汰

## 最佳实践

### 1. 合理设置TTL

```kotlin
// 短期数据：用户会话
cacheTemplate.put("session:$sessionId", sessionData, Duration.ofHours(2))

// 中期数据：产品信息
cacheTemplate.put("product:$productId", productData, Duration.ofMinutes(30))

// 长期数据：系统配置
cacheTemplate.put("config:$configKey", configData, Duration.ofDays(1))
```

### 2. 使用命名空间隔离

```kotlin
// 不同模块使用不同命名空间
cacheTemplate.getOrPut("products", "product:$id", Product::class.java, supplier)
cacheTemplate.getOrPut("orders", "order:$id", Order::class.java, supplier)
cacheTemplate.getOrPut("users", "user:$id", User::class.java, supplier)
```

### 3. 缓存键设计

```kotlin
// 良好的缓存键设计
"user:$userId:profile"
"product:$productId:details"
"order:$orderId:status"

// 避免冲突
"cache:v1:user:$userId"  // 包含版本号
```

### 4. 缓存一致性

```kotlin
@Service
class ProductService {
    fun updateProduct(product: Product): Product {
        val updated = productRepository.save(product)

        // 立即清除相关缓存
        cacheTemplate.remove("products", "product:${updated.id}")
        cacheTemplate.clear("product-list")  // 清除列表缓存

        return updated
    }
}
```

### 5. 缓存预热

```kotlin
@EventListener
class CacheWarmupListener {

    @EventListener(ApplicationReadyEvent::class)
    fun warmupCache() {
        // 预加载热点数据
        cacheTemplate.getOrPut("hot-products", "list", List::class.java, {
            productService.getHotProducts()
        }, Duration.ofMinutes(10))
    }
}
```

## 监控和统计

系统内置了缓存统计功能：

```kotlin
@Component
class CacheMonitor {

    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "hit_rate" to cacheManager.hitRate,
            "size" to cacheManager.size,
            "hit_count" to cacheManager.hitCount,
            "miss_count" to cacheManager.missCount
        )
    }
}
```

## 性能优化建议

1. **合理设置缓存大小**：避免内存溢出，建议设置为可用内存的 20-30%
2. **选择合适的TTL**：平衡数据新鲜度和缓存命中率
3. **使用异步操作**：对于非关键路径，使用异步缓存操作
4. **启用压缩**：对于大对象，启用压缩可节省内存
5. **监控缓存效果**：定期检查命中率和内存使用情况

## 扩展和替换

由于使用了抽象接口设计，可以轻松替换底层实现：

```kotlin
// 替换为 Redis 实现
@Configuration
class RedisCacheConfig {
    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        return RedisCacheManager()
    }
}
```

## 故障排查

### 常见问题

1. **缓存未命中**
   - 检查键名是否正确
   - 确认TTL是否过期
   - 验证命名空间是否正确

2. **内存不足**
   - 检查max-size设置
   - 考虑启用压缩
   - 优化TTL设置

3. **性能问题**
   - 启用异步操作
   - 检查淘汰策略
   - 监控统计信息

### 日志配置

```yaml
logging:
  level:
    dev.yidafu.aqua.common.cache: DEBUG
```

## 示例代码

查看 `CachedOrderServiceExample.kt` 了解完整的缓存使用示例。