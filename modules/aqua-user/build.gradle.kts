plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("com.ewerk.gradle.plugins.querydsl") version "1.0.10"
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))

  // Spring Data JPA for AddressRepository
  implementation(libs.bundles.spring.boot.data)

  // User-specific dependencies
  implementation(libs.spring.boot.starter.security)
  implementation(libs.bundles.graphql)

  // HTTP client for WeChat API
  implementation(libs.spring.boot.starter.web)

  // Mappie
  implementation(libs.mappie.api)

  // QueryDSL for type-safe queries (temporarily disabled)
   implementation(libs.bundles.querydsl)
//   annotationProcessor(libs.querydsl.apt)
  implementation(libs.wechat.miniapp)
}
val querydslDir = "$buildDir/generated/querydsl"

querydsl {
  jpa = true
  hibernate = true
  querydslSourcesDir = querydslDir
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
