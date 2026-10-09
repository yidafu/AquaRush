plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P4:仓储全部改写为 Exposed DSL,摘除 QueryDSL/KSP
  id("aqua.kotlin.exposed")
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

  // 仓储实现 H2 实现级测试(aqua.kotlin.exposed 不带 test 依赖)
  testImplementation(libs.h2)
}
