plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Mappie
  implementation(libs.mappie.api)

  // Spring Boot(barrel 子域模块内解析器需 web/security/validation/actuator)
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // GraphQL(barrel 子域模块内解析器)
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)

  // Test
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation(libs.springmockk)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
