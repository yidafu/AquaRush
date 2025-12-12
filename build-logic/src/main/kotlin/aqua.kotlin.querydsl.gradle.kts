// Plugin that provides QueryDSL dependencies and configuration
plugins {
  id("com.ewerk.gradle.plugins.querydsl")
  kotlin("kapt")
}

// Add QueryDSL dependencies
dependencies {
  add("implementation", "com.querydsl:querydsl-jpa:5.1.0:jakarta")
  add("implementation", "com.querydsl:querydsl-core:5.1.0")
  add("kapt", "com.querydsl:querydsl-apt:5.1.0:jakarta")
}

// Note: The QueryDSL configuration (querydsl { ... }) needs to be applied
// in the target project's build.gradle.kts file since the extension
// is not available during build-logic compilation.