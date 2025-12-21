plugins {
  id("aqua.kotlin.spring")
}

dependencies {
  // Spring Boot starter for basic logging support
//  implementation("org.springframework.boot:spring-boot-starter")

  // Spring Data JPA for shared entities
  implementation(libs.bundles.spring.boot.data)

  // Explicitly add Hibernate core for annotations
  implementation("org.hibernate.orm:hibernate-core:6.4.1.Final")

  // Spring Security for JWT authentication
  implementation(libs.bundles.spring.boot.security)

  // GraphQL support
  implementation(libs.bundles.graphql)

  // Spring Messaging support
  implementation(libs.bundles.messaging)

  // Reactor Netty for reactive support
  implementation(libs.reactor.core)

  // Tracing support
  implementation(libs.micrometer.tracing.bridge.brave)

  // Spring Boot Actuator for health checks
  implementation(libs.spring.boot.starter.actuator)

  implementation(libs.spring.boot.starter.aop)

  implementation(libs.bundles.jackson)
  // Validation
  implementation("jakarta.validation:jakarta.validation-api:4.0.0-M1")

  // JWT support
  implementation(libs.bundles.jwt)

  // MapDB for caching
  implementation(libs.mapdb)
  testImplementation(libs.spring.boot.starter.test)
  implementation(libs.wechat.miniapp)
  // QueryDSL for type-safe queries (temporarily disabled)
  // implementation(libs.bundles.querydsl)
  // annotationProcessor(libs.querydsl.apt)
}

// Configure Kotlin compilation to include generated source
kotlin {
  sourceSets {
    main {
      kotlin.srcDir("src/main/graphql-gen")
      // Add QueryDSL generated sources
      kotlin.srcDir(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
    }
  }
}
