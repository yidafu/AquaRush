plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-user"))

  // GraphQL dependencies
  implementation(libs.bundles.graphql)

  // Security dependencies
  implementation(libs.spring.boot.starter.security)

  // Add Spring Web dependency for REST controllers
  implementation(libs.spring.boot.starter.web)

  // Apache Commons CSV for export functionality
  implementation("org.apache.commons:commons-csv:1.10.0")

  // Mappie
  implementation(libs.mappie.api)

  // Hibernate Types for JSON support
//  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")

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
