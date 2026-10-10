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

package dev.yidafu.aqua.client.testutil

import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource

/**
 * 把 client 的 `@SpringBootTest` 集成测试数据源指向 **Testcontainers 真实 PostgreSQL**。
 *
 * 为什么不是 `src/test/resources/application-test.yml`：仓库 `.gitignore` 用
 * `application-*.yml` 把 profile 配置整体排除在版本控制外（避免提交本机/密钥配置），
 * 所以测试库配置**不能**靠 yml 落地 —— 否则全新 clone / CI 上根本没有该文件，
 * 测试会悄悄连到 `application.yml` 里的本机 `localhost:5432` 库。改由版本受控的
 * [ApplicationContextInitializer] 提供，任何环境下都自带数据库。
 *
 * 注册方式：`src/test/resources/META-INF/spring.factories`
 * （SpringApplication 启动时自动加载，无需改 30+ 个测试类）。
 *
 * 优先级：这里用 `propertySources.addFirst` 写入，高于 `application.yml` /
 * 本机残留的 `application-test.yml` / `@TestPropertySource`，因此本机残留配置不再生效。
 *
 * **表结构来源**：client 生产不跑 Liquibase（schema 由 admin 服务维护），JPA/Hibernate
 * 也随 Exposed 迁移移除 —— 因此测试表结构由 [ClientTestSchema] 在上下文初始化前按
 * foundation 的 Exposed 表定义创建（见该类注释）。
 *
 * 容器生命周期：库是容器内一次性库，`drop-first` 只在上下文启动阶段执行，退出阶段
 * 不依赖数据库（避免 Testcontainers 关闭钩子与 Hibernate/连接池在 JVM shutdown 竞争）。
 */
class PostgresTestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    val coordinates = PostgresTestContainer.coordinates(ClientTestSchema.DB_NAME)
    ClientTestSchema.createIfNeeded(PostgresTestContainer.connect(ClientTestSchema.DB_NAME))
    applicationContext.environment.propertySources.addFirst(
      MapPropertySource(
        "aquaTestcontainersPostgres",
        mapOf(
          "spring.datasource.url" to coordinates.url,
          "spring.datasource.username" to coordinates.user,
          "spring.datasource.password" to coordinates.password,
          "spring.datasource.driver-class-name" to "org.postgresql.Driver",
          // client 无 Liquibase changelog(schema 由 admin 服务维护,生产 enabled=false),
          // JPA/Hibernate 已随 Exposed 迁移移除 → 测试表结构由 ClientTestSchema 按
          // foundation 的 Exposed 表定义创建(见该类注释)。
          "spring.liquibase.enabled" to "false",
        ),
      ),
    )
  }
}
