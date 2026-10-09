/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.common.infra.exposed

import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

/**
 * Exposed(1.x) 接入基建：JPA -> Exposed 迁移期间，为「已迁移模块」提供 Exposed 的 [Database]。
 *
 * 设计约定（迁移方案 §5.1，务必先读再改）：
 * 1. **只暴露 [Database]，不注册 `SpringTransactionManager`**。
 *    Exposed 的 `SpringTransactionManager` 会在内部再 `Database.connect(dataSource)` 建出**第二个**
 *    Database 实例；而 Exposed 的 `transaction { }` 按「默认 Database」查找事务。两个 Database 并存
 *    会让仓储查不到 Spring 事务里存的 Exposed 事务，从而静默另开连接 —— 比不加集成更危险。
 *    因此本阶段用「仓储/服务自己用 `transaction { }` 划边界」的语义，等写核心(trade/product/marketing)
 *    迁移、需要 `@Transactional` 跨多个仓储时，再引入 `SpringTransactionManager` 并保证全局只有一个
 *    Database（迁移方案 P4）。
 * 2. **绝不注册主事务管理器**：`@Transactional` 的解析必须继续落在 JPA 的 `JpaTransactionManager` 上，
 *    否则仍是 JPA 的 166 个仓储会静默走错事务管理器。
 * 3. 不用官方 `exposed-spring-boot4-starter`：其自动配置会注册主事务管理器（同上原因）。
 *
 * 激活条件：classpath 上有 Exposed JDBC + 容器里存在 [DataSource]（由 Spring Boot 的
 * `DataSourceAutoConfiguration` 提供，故必须 `@AutoConfigureAfter`，否则条件求值早于数据源注册）。
 * [Database] 的创建本身**不建立数据库连接**，连接在首次执行事务时才获取。
 */
@AutoConfiguration
@AutoConfigureAfter(name = ["org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"])
@ConditionalOnClass(Database::class)
class ExposedAutoConfiguration {
  @Bean
  @ConditionalOnBean(DataSource::class)
  @ConditionalOnMissingBean(Database::class)
  fun exposedDatabase(dataSource: DataSource): Database = Database.connect(datasource = dataSource)
}
