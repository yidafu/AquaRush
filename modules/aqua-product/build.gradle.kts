plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // GraphQL dependencies
  implementation(libs.bundles.graphql)

  // Security dependencies
  implementation(libs.spring.boot.starter.security)

  // Add Spring Web dependency for REST controllers
  implementation(libs.spring.boot.starter.web)

  // Apache Commons CSV for export functionality
  implementation("org.apache.commons:commons-csv:1.10.0")

  // Apache POI for Excel export functionality
  implementation("org.apache.poi:poi:5.2.5")
  implementation("org.apache.poi:poi-ooxml:5.2.5")

  // Mappie
  implementation(libs.mappie.api)

  // Hibernate Types for JSON support
//  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")

  // QueryDSL dependencies are handled by aqua.kotlin.querydsl plugin
}

// QueryDSL configuration is now handled by aqua.kotlin.querydsl plugin
