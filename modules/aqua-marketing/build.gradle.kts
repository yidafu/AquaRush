plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Mappie
  implementation(libs.mappie.api)

  // Test
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation(libs.springmockk)
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
