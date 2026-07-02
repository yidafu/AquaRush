plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  alias(libs.plugins.ksp)
  id("aqua.kotlin.querydsl")
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

  // Mappie
  implementation(libs.mappie.api)

  // QueryDSL dependencies are handled by aqua.kotlin.querydsl plugin
  implementation(libs.wechat.miniapp)

  // Used by WeChat code (kept after notice migration)
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.http.client)

  // Test dependencies
  testImplementation(libs.h2)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
