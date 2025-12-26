plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
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
}

// Configure QueryDSL
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
