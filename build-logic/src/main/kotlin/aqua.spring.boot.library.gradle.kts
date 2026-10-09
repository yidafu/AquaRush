plugins {
  id("aqua.kotlin.spring")
  id("org.springframework.boot")
}

// Disable bootJar for library modules
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  enabled = false
}

// Enable plain JAR
tasks.named<Jar>("jar") {
  enabled = true
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  // JPA→Exposed 迁移 P5:JPA/QueryDSL 已下线,数据访问统一走 Exposed(JDBC)。
  // 这里保留 DataSource/JdbcTemplate/事务管理器所需的最小 Spring JDBC 栈,
  // 以及仓储接口仍在使用的 Spring Data 分页抽象(Page/Pageable/Sort → spring-data-commons)。
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.data:spring-data-commons")
}
