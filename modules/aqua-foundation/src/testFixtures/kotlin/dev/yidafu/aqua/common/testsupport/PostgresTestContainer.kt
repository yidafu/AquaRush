/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.common.testsupport

import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.ConcurrentHashMap

/**
 * 各模块仓储/集成测试共用的 **Testcontainers 真实 PostgreSQL** 容器基建
 * （`java-test-fixtures` 提供给各业务模块的 test 源集，见 aqua-foundation/build.gradle.kts）。
 *
 * ## 为什么不再用 H2
 *
 * H2 的 `MODE=PostgreSQL` 兼容模式只是 SQL 方言的近似，测不出生产 PG 的真实行为：
 * - `INSERT ... ON CONFLICT`(幂等 upsert)在 H2 走 `MERGE`，冲突时行锁与计数递增语义不同；
 * - `jsonString` 列绑定、varchar 长度校验、`timestamptz`/numeric 类型转换等方言差异；
 * - 唯一索引冲突、并发行锁、序列行为。
 * 「H2 单线程跑通」不背书生产 PG 跑通，因此仓储实现级测试一律打真实 PG。
 *
 * ## 生命周期与并发约定
 *
 * 每个 **JVM 每个库名** 一个容器（`./gradlew` 每个模块的 test 任务都是独立 JVM，
 * 所以 `./gradlew check` 并行时会同时存在多个 PG 容器）。容器由 Testcontainers 的
 * JVM shutdown hook 停止。
 *
 * 镜像与容器参数刻意保守 —— 本机 podman machine 只分配了 2GB 内存，而 `check`
 * 会并行拉起多个模块的容器，默认 `shared_buffers=128MB` 会把 VM 打爆（表现为后续
 * 容器 `pg_isready` 超时或 OOM）。测试库也不需要对崩溃安全的持久性。
 *
 * **全表清理（各模块 `reset()` 的 `deleteAll()`）不是线程安全的**，依赖 Kotest 串行
 * 执行；若将来开启 spec 并行需改造。
 */
object PostgresTestContainer {
  /**
   * 镜像默认 `postgres:17-alpine`；由 `aqua.testcontainers` 约定插件以系统属性下发，
   * 未走 Gradle（IDE 直跑）时回退到环境变量 `AQUA_TEST_PG_IMAGE`，最后回退默认值。
   */
  private val image: String =
    System.getProperty("aqua.test.pg.image")
      ?: System.getenv("AQUA_TEST_PG_IMAGE")
      ?: "postgres:17-alpine"

  /**
   * 测试实例的 postgres 参数：
   * - `fsync`/`full_page_writes`/`synchronous_commit` 关掉 —— 临时测试库不需要崩溃恢复，
   *   DDL 密集的建表/删表能快一个数量级；
   * - `shared_buffers` 压到 16MB —— 见类注释的内存约束；
   * - `max_connections` 提到 200 —— 单个 service 集成测试会有多个 Spring 缓存上下文，
   *   每个上下文各持一个 Hikari 池，PG 默认 100 会被打满。
   */
  private val postgresArgs: Array<String> =
    arrayOf(
      "postgres",
      "-c",
      "fsync=off",
      "-c",
      "full_page_writes=off",
      "-c",
      "synchronous_commit=off",
      "-c",
      "shared_buffers=16MB",
      "-c",
      "max_connections=200",
    )

  private val containers = ConcurrentHashMap<String, PostgreSQLContainer<*>>()

  /** 容器里的一个数据库的 JDBC 坐标(不含任何 Testcontainers / Exposed 类型)。 */
  data class Coordinates(
    val url: String,
    val user: String,
    val password: String,
  )

  /**
   * 启动（或复用）[databaseName] 对应的容器，并返回连到它的 Exposed [Database]。
   *
   * 调用方各模块的 `*PgTestSupport` 在 object 初始化时调用一次即可；同一个库名重复
   * 调用只会新建 Exposed Database 包装，不会重复起容器。
   */
  fun connect(databaseName: String): Database {
    val coordinates = coordinates(databaseName)
    return Database.connect(
      url = coordinates.url,
      driver = "org.postgresql.Driver",
      user = coordinates.user,
      password = coordinates.password,
    )
  }

  /**
   * 同上，但返回裸 JDBC 坐标 —— 供两个 service 模块把 Spring 的 `spring.datasource.*`
   * 指到这个容器（见各服务的 `PostgresTestContextInitializer`）。
   */
  fun coordinates(databaseName: String): Coordinates {
    val container = containers.computeIfAbsent(databaseName) { start(it) }
    return Coordinates(
      url = container.jdbcUrl,
      user = container.username,
      password = container.password,
    )
  }

  private fun start(databaseName: String): PostgreSQLContainer<*> =
    PostgreSQLContainer(image).apply {
      withDatabaseName(databaseName)
      withCommand(*postgresArgs)
      start()
    }
}
