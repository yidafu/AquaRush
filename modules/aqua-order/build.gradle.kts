plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
}

dependencies {
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-user"))
  implementation(project(":modules:aqua-product"))
  implementation(project(":modules:aqua-delivery"))
  implementation(project(":modules:aqua-payment"))

  // GraphQL support
  implementation(libs.bundles.graphql)

  // Spring Security
  implementation(libs.bundles.spring.boot.security)

  // Validation
  implementation(libs.spring.boot.starter.validation)

  // Order-specific dependencies can be added here

  // Mappie
  implementation(libs.mappie.api)

  // QueryDSL for type-safe queries (temporarily disabled)
  // implementation(libs.bundles.querydsl)
  // annotationProcessor(libs.querydsl.apt)
}

// Configure Kotlin compilation to include generated source
kotlin {
  sourceSets {
    main {
      // Add QueryDSL generated sources
      kotlin.srcDir(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
    }
  }
}
