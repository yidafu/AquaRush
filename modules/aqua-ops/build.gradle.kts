plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  // Exposed(1.x) 接入：Database 基建 + 依赖，见仓库根 AquaRush-JPA-to-Exposed-迁移方案.md
  id("aqua.kotlin.exposed")
  // QueryDSL/KSP 已随 JPA -> Exposed 迁移移除（P1：仓储全部改为 Exposed DSL）
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Mappie
  implementation(libs.mappie.api)

  // Test
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation(libs.springmockk)
  // 仓储实现测试（H2 + Exposed）：P1 迁移后仓储成为模块内真实代码，必须有实现级回归
  testImplementation(libs.h2)
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
