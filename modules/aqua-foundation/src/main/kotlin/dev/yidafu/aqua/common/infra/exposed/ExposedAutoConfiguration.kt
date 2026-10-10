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
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.spring7.transaction.ExposedSpringTransactionAttributeSource
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Role
import javax.sql.DataSource

/**
 * Exposed(1.x) 接入基建(JPA→Exposed 迁移 P5 收口后的**唯一**数据访问实现)。
 *
 * 提供三件事:主事务管理器、与它同源的 [Database]、以及让 `ExposedSQLException` 触发回滚的
 * `TransactionAttributeSource`(与官方 `exposed-spring-boot4-starter` 的做法一致)。
 *
 * ## 为什么把 [Database] bean 设成 manager 内部的实例(关键)
 *
 * `SpringTransactionManager` 在构造时自己 `Database.connect(dataSource)` 建出内部 Database,
 * 而 Exposed 的 `JdbcTransactionManager.currentOrNull()` 是**按 Database 实例过滤**的:
 * `holder.getTransactionOrNull(db) { it.db == db }`。若应用侧再 `Database.connect` 出一个不同的
 * 实例,仓储里的 `transaction(appDb) { }` 找不到 manager 开的事务,会**另开顶层事务并提交**
 * —— 表现就是「`@Transactional` 抛异常后数据仍在」(实测复现)。
 *
 * 因此这里不再自行 connect,而是让 manager 先建库,再把 `TransactionManager.primaryDatabase`
 * (最近注册的 Database = manager 的内部实例)作为 bean 暴露给仓储。全局只有一个 Database。
 *
 * ## 事务闭合的其余事实(已核对 spring7-transaction 1.5.0 源码)
 * - 仓储的 `transaction(database) { }` 在已有事务时经 `newTransaction` 返回**外层事务**
 *   (`useNestedTransactions=false` 默认),不新开连接,异常一起回滚;未处于 Spring 事务时
 *   (异步消费者、定时任务、单测)仍按原语义自建顶层事务。
 * - 全仓数据访问**只经 Exposed**:需要裸 JDBC 的少数场景(`LogPartitionRepositoryImpl` 的
 *   `COPY ... TO STDOUT`)从当前事务取连接(`Transaction.connection.connection`),不再依赖
 *   「`doBegin` 把连接以 Spring `ConnectionHolder` 绑定到 [DataSource]」这一隐式约定 ——
 *   事务边界与连接来源都收敛到 Exposed 一处。
 *
 * `@AutoConfigureBefore` 让 Spring Boot 的 `DataSourceTransactionManagerAutoConfiguration`
 * 看到已有 `PlatformTransactionManager` 而退让;`@Primary` 作为双保险。
 */
@AutoConfiguration
@AutoConfigureAfter(name = ["org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"])
@AutoConfigureBefore(name = ["org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"])
@ConditionalOnClass(Database::class)
class ExposedAutoConfiguration {
  /**
   * 主事务管理器:`@Transactional` 开 Exposed 事务,仓储的 `transaction(database) { }` 加入同一事务。
   * manager 构造时会 connect 出内部 Database,故本 bean 必须先于 [exposedDatabase] 创建(方法参数即依赖)。
   */
  @Bean
  @Primary
  fun exposedTransactionManager(dataSource: DataSource): SpringTransactionManager = SpringTransactionManager(dataSource)

  /**
   * 仓储注入的 [Database]:与 [exposedTransactionManager] 内部实例**同一对象**
   * (见类注释;自行 `Database.connect` 会导致事务不闭合)。
   */
  @Bean
  @ConditionalOnMissingBean(Database::class)
  fun exposedDatabase(transactionManager: SpringTransactionManager): Database =
    TransactionManager.primaryDatabase
      ?: error("Exposed SpringTransactionManager 未注册 Database,无法提供 Database bean")

  /**
   * `ExposedSQLException` 继承自受检的 `java.sql.SQLException`,Spring 默认**不会**为受检异常回滚
   * → 数据库报错会静默提交前面的写入。这里把 `SQLException` 加入回滚规则(同官方 starter)。
   */
  @Bean
  @Primary
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  fun exposedSpringTransactionAttributeSource(): ExposedSpringTransactionAttributeSource = ExposedSpringTransactionAttributeSource()
}
