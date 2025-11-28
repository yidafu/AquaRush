plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
}

dependencies {
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))

  // GraphQL support for payment resolvers
  implementation(libs.bundles.graphql)

  // Spring Security for security context
  implementation(libs.spring.boot.starter.security)

  // Note: Order-related operations will be handled through common-module
  // to avoid circular dependencies

  // WeChat Pay SDK
  implementation(libs.wechatpay.sdk)

  // Mappie
  implementation(libs.mappie.api)

  testImplementation(project(":modules:aqua-common"))

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
