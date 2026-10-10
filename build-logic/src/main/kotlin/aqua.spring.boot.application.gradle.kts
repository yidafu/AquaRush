plugins {
    id("aqua.kotlin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Configure bootJar task
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
    // mainClass 由各服务自行声明(services/*/build.gradle.kts 的 springBoot { mainClass }),
    // 不再在此硬编码,否则会覆盖服务级配置导致 java -jar 启动失败。
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
    // JPA→Exposed 迁移 P5:JPA 已下线,服务侧数据访问走 Exposed(JDBC);
    // 保留 Spring JDBC 栈(DataSource/JdbcTemplate/事务管理器)与仓储接口使用的分页抽象。
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.data:spring-data-commons")
}
