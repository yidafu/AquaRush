plugins {
  id("aqua.spring.boot.library")
  // Exposed(1.x) 接入：Database 基建 + 依赖，见仓库根 AquaRush-JPA-to-Exposed-迁移方案.md
  id("aqua.kotlin.exposed")
  // Testcontainers 真实 PG 测试(容器运行时接线见 build-logic/aqua.testcontainers.gradle.kts)
  id("aqua.testcontainers")
  // QueryDSL/KSP 已随 JPA -> Exposed 迁移移除（P1：仓储全部改为 Exposed DSL）
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Test
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation(libs.springmockk)
  // 仓储实现测试（Testcontainers 真实 PG + Exposed）：P1 迁移后仓储成为模块内真实代码，
  // 必须有实现级回归；共享 PG 基建在 foundation testFixtures
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
}
