plugins {
  id("aqua.kotlin.spring")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.querydsl")
}

dependencies {
  implementation(project(":modules:aqua-common"))
  implementation(project(":modules:aqua-api"))
  implementation(project(":modules:aqua-logging"))
  implementation(project(":modules:aqua-delivery"))
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.data)
  implementation(libs.bundles.graphql)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.http.client)
  implementation(libs.bundles.cache)
  implementation(libs.postgresql)
  // Validation
  implementation(libs.spring.boot.starter.validation)

  // For Redis cache
  implementation(libs.bundles.redis)

  // Mappie
  implementation(libs.mappie.api)

  testImplementation(libs.spring.boot.starter.test)
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
