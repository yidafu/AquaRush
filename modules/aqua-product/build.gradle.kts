plugins {
  id("aqua.spring.boot.library")
  // JPA→Exposed 迁移 P4:仓储全部改写为 Exposed DSL,摘除 QueryDSL/KSP
  id("aqua.kotlin.exposed")
  // 仓储实现级测试打 Testcontainers 真实 PG(共享基建见 aqua-foundation testFixtures)
  id("aqua.testcontainers")
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

  // 仓储实现级测试用真实 PG 支撑(PostgresTestContainer 在 foundation testFixtures)
  testImplementation(testFixtures(project(":modules:aqua-foundation")))
}
