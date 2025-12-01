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
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  version.set("1.5.0")
  debug.set(false)
}
