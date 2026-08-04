plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.ktlint)
}

group = "dev.yidafu.aqua"
version = "0.1.0-SNAPSHOT"

repositories {
  maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
  mavenCentral()
  maven { url = uri("https://repo.spring.io/milestone") }
  maven { url = uri("https://repo.spring.io/snapshot") }
  maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

// Apply BOM to all subprojects
dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    mavenBom("tools.jackson:jackson-bom:3.0.2")
  }
}

configurations.all {
  resolutionStrategy {
    force(
      "com.fasterxml.jackson:jackson-bom:3.0.2",
//      "com.fasterxml.jackson:jackson-bom:2.20.1",
//      "tools.jackson:jackson-bom:3.0.2",
      "ccom.fasterxml.jackson.core:jackson-annotations:2.20",
    )
  }
}
subprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    gradlePluginPortal()
  }

  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  // The ktlint version must be configured per-project. Setting it on the root
  // project's KtlintExtension does NOT propagate to subprojects, so they fall back
  // to the plugin's default (1.0.1), which is incompatible with Kotlin 2.x
  // (ktlint 1.0.1 references KtTokens.HEADER_KEYWORD, removed in Kotlin 2.2+).
  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.5.0")
    debug.set(false)
    filter {
      // Generated sources (QueryDSL/KSP output under build/generated, GraphQL
      // codegen output under src/main/graphql-gen) must not be linted. They are
      // also outputs of compileJava, so including them makes Gradle 9 fail the
      // ktlint tasks with an implicit-dependency validation error.
      // The actual per-file exclusion of generated code is handled by the
      // [**/graphql-gen/**] and [**/build/generated/**] sections in .editorconfig,
      // since ktlint matches those globs against project-relative paths while
      // this filter matches against per-source-root paths.
      exclude("**/build/generated/**")
    }
  }

  // JPA all-open is configured via the aqua.kotlin.jpa convention plugin,
  // which applies kotlin("plugin.allopen") and registers @Entity / @MappedSuperclass
  // / @Embeddable as all-open annotations. Using -Xallopen / -Xannotation flags here
  // is unsupported in Kotlin 2.x and emits warnings.
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  version.set("1.5.0")
  debug.set(false)
}

// Disable bootJar task in root project (root is not an application)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  enabled = false
}
