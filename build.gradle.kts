import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.kover)
}

group = "dev.yidafu.aqua"
version = "0.1.0-SNAPSHOT"

repositories {
  maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
  mavenCentral()
  maven { url = uri("https://repo.spring.io/milestone") }
  maven { url = uri("https://repo.spring.io/snapshot") }
  maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

// Apply BOM to all subprojects
dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    mavenBom("tools.jackson:jackson-bom:3.0.2")
  }
}

configurations.all {
  resolutionStrategy {
    force(
      "com.fasterxml.jackson:jackson-bom:3.0.2",
//      "com.fasterxml.jackson:jackson-bom:2.20.1",
//      "tools.jackson:jackson-bom:3.0.2",
      "ccom.fasterxml.jackson.core:jackson-annotations:2.20",
    )
  }
}
subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    gradlePluginPortal()
  }

  // 测试框架已从 JUnit 5 切换为 Kotest（Kotest 运行在 JUnit Platform 上）。
  // 全局排除 JUnit Jupiter/Vintage 相关依赖，避免与 Kotest 混用。
  configurations.all {
    exclude(group = "org.junit.jupiter")
    exclude(group = "org.junit.vintage")
    exclude(group = "org.mockito", module = "mockito-junit-jupiter")
  }

  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  // The ktlint version must be configured per-project. Setting it on the root
  // project's KtlintExtension does NOT propagate to subprojects, so they fall back
  // to the plugin's default (1.0.1), which is incompatible with Kotlin 2.x
  // (ktlint 1.0.1 references KtTokens.HEADER_KEYWORD, removed in Kotlin 2.2+).
  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.5.0")
    debug.set(false)
    filter {
      // Generated sources (QueryDSL/KSP output under build/generated, GraphQL
      // codegen output under src/main/graphql-gen) must not be linted. They are
      // also outputs of compileJava, so including them makes Gradle 9 fail the
      // ktlint tasks with an implicit-dependency validation error.
      // The actual per-file exclusion of generated code is handled by the
      // [**/graphql-gen/**] and [**/build/generated/**] sections in .editorconfig,
      // since ktlint matches those globs against project-relative paths while
      // this filter matches against per-source-root paths.
      exclude("**/build/generated/**")
    }
  }

  // JPA all-open is configured via the aqua.kotlin.jpa convention plugin,
  // which applies kotlin("plugin.allopen") and registers @Entity / @MappedSuperclass
  // / @Embeddable as all-open annotations. Using -Xallopen / -Xannotation flags here
  // is unsupported in Kotlin 2.x and emits warnings.
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  version.set("1.5.0")
  debug.set(false)
}

// Disable bootJar task in root project (root is not an application)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  enabled = false
}

// ================================================================
// Kover 测试覆盖率门禁（标准详见 claude/deployment.md §Testing Strategy）
// ----------------------------------------------------------------
// 分模块行覆盖率阈值（开发中必须达到的标准）：
//   Tier S 80% : aqua-trade, aqua-marketing —— 订单/支付/营销（钱与状态机，最高优先级）
//   Tier A 70% : aqua-ops, aqua-user, aqua-product, aqua-analytics
//   Tier B 60% : aqua-foundation, aqua-platform
//   Tier C 55% : aqua-admin, aqua-client —— 以 GraphQL 集成测试为主，Resolver 为薄壳
// 统一排除项（均为无业务逻辑或生成代码，避免稀释真实数字）：
//   - DTO 请求/响应体（dev.yidafu.aqua.api.dto）
//   - GraphQL 生成代码（dev.yidafu.aqua.common.graphql.generated）
//   - Mappie 映射类（*Mapper*，编译期生成实现）
//   - 异常工厂（*Exception / *Exceptions）
//   - 启动类（*Application）
//   - @Configuration 配置类
// 说明：Kover 校验任务默认挂接在 `check` 上，`./gradlew build` / `check` 不达标即失败。
// ================================================================
val koverModuleBounds: Map<String, Int> =
  mapOf(
    "aqua-trade" to 80,
    "aqua-marketing" to 80,
    "aqua-ops" to 70,
    "aqua-user" to 70,
    "aqua-product" to 70,
    "aqua-analytics" to 70,
    "aqua-foundation" to 60,
    "aqua-platform" to 60,
    "aqua-admin" to 55,
    "aqua-client" to 55,
  )

kover {
  // 合并所有子模块覆盖率数据到根工程，生成全库统一报告（配置缓存未开启，merge 可用）
  merge {
    subprojects()
  }
  reports {
    // 全库总体阈值（所有模块合并后的总覆盖率）
    verify {
      rule {
        minBound(60)
      }
    }
  }
}

subprojects {
  // 给每个子模块应用 Kover 并配置分模块门禁（阈值见 koverModuleBounds）
  pluginManager.apply("org.jetbrains.kotlinx.kover")
  extensions.configure<KoverProjectExtension> {
    reports {
      filters {
        excludes {
          packages(
            "dev.yidafu.aqua.api.dto",
            "dev.yidafu.aqua.common.graphql.generated",
            // Exposed 基建（JPA -> Exposed 迁移，方案 §5.1）：自动配置无业务逻辑，
            // 其行为由 services 层集成测试与 ops 仓储实现测试间接验证
            "dev.yidafu.aqua.common.infra.exposed",
          )
          classes("*Mapper*", "*Exception", "*Exceptions", "*Application", "*SoftDeletableTable")
          annotatedBy("org.springframework.context.annotation.Configuration")
        }
      }
      verify {
        rule {
          minBound(koverModuleBounds[name] ?: 60)
        }
      }
    }
  }

  // 集成测试(Spring 上下文 + Artemis + H2)在默认 512m worker 堆下会 OOM,
  // 显式分配 1g 保证测试进程稳定运行。
  tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
  }
}
