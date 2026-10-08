// Plugin that provides QueryDSL dependencies using BOM
// Note: Apply com.google.devtools.ksp plugin in the target project before applying this plugin

plugins {
  `java-library`
  kotlin("jvm")

  kotlin("plugin.jpa")
  id("com.google.devtools.ksp")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("io.github.openfeign.querydsl:querydsl-bom:7.1"))
  implementation("jakarta.persistence:jakarta.persistence-api")
  implementation("io.github.openfeign.querydsl:querydsl-jpa")
  ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:7.1")
  // Note: ksp dependency should be added by the consuming module
}
