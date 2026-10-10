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
 * 本机残留的 `application-test.yml` / `@TestPropertySource`，因此本机残留的 H2
 * 配置不再生效。
 *
 * `ddl-auto=create` + `liquibase.enabled=false` 刻意保留既有测试语义（表结构由
 * Hibernate 实体元数据生成，启动时 drop+create）。切到「跑真实 Liquibase changelog」
 * 是另一个独立议题，不在本次 H2→Testcontainers 迁移范围内。
 *
 * 为什么是 `create` 而不是原来的 `create-drop`：**容器生命周期**。`create-drop` 会在
 * SessionFactory 关闭时再连库执行 DROP —— 那是 JVM shutdown 阶段，而 Testcontainers
 * 自己的 shutdown hook 也在同一阶段停容器，二者并发竞争：容器先停 → Hibernate 拿不到
 * 连接，Hikari 连接池无限重试 → SpringApplicationShutdownHook 永不返回 →
 * `./gradlew check` **挂死**。测试库是容器内的一次性库，退出时 drop 没有价值，因此改成
 * `create`（启动时仍然 drop+create，共享同一套库的语义不变），去掉退出阶段对数据库的依赖。
 */
class PostgresTestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    val coordinates = PostgresTestContainer.coordinates("aquaclienttest")
    applicationContext.environment.propertySources.addFirst(
      MapPropertySource(
        "aquaTestcontainersPostgres",
        mapOf(
          "spring.datasource.url" to coordinates.url,
          "spring.datasource.username" to coordinates.user,
          "spring.datasource.password" to coordinates.password,
          "spring.datasource.driver-class-name" to "org.postgresql.Driver",
          "spring.jpa.hibernate.ddl-auto" to "create",
          "spring.jpa.generate-ddl" to "true",
          // 本机残留的 application-test.yml 可能把方言钉成 H2Dialect;连接已是真实 PG,
          // 方言不对会按 H2 生成 DDL(表建不出来),必须显式钉成 PostgreSQL。
          "spring.jpa.properties.hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
          // main application.yml 用 metadata-then-scripts(生产靠 Liquibase 脚本),
          // 但测试禁用 Liquibase 且没有 scripts 源 → 必须显式改回 metadata,
          // 否则 Hibernate 报 "Unrecognized schema generation source type"。
          "spring.jpa.properties.jakarta.persistence.schema-generation.create-source" to "metadata",
          "spring.jpa.properties.jakarta.persistence.schema-generation.drop-source" to "metadata",
          "spring.liquibase.enabled" to "false",
        ),
      ),
    )
  }
}
