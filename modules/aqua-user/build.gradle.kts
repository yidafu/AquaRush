plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  alias(libs.plugins.ksp)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-logging"))

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
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
