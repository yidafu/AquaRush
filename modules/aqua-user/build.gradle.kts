plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P3:仓储全部改写为 Exposed DSL,摘除 QueryDSL/KSP
  id("aqua.kotlin.exposed")
  // Testcontainers 真实 PG 测试(容器运行时接线见 build-logic/aqua.testcontainers.gradle.kts)
  id("aqua.testcontainers")
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Spring Data JPA for AddressRepository
  implementation(libs.bundles.spring.boot.data)

  // User-specific dependencies
  implementation(libs.spring.boot.starter.security)
  implementation(libs.bundles.graphql)

  // HTTP client for WeChat API
  implementation(libs.spring.boot.starter.web)

  implementation(libs.wechat.miniapp)

  // Used by WeChat code (kept after notice migration)
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.http.client)

  // Test dependencies
  // 仓储实现级测试打 Testcontainers 真实 PG(共享基建在 foundation testFixtures)
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
}
