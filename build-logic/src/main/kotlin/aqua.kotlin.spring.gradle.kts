plugins {
    id("aqua.kotlin.common")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot BOM is now managed at root level via dependencyManagement
    implementation("tools.jackson.module:jackson-module-kotlin")

    // spring-boot-starter-test 提供 spring-test/AssertJ 等; 排除 JUnit Jupiter
    // （测试框架已切换为 Kotest, JUnit 相关模块不再需要）
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.vintage")
        exclude(group = "org.mockito", module = "mockito-junit-jupiter")
    }
    testImplementation("io.mockk:mockk:1.13.10")
    // Kotest Spring 扩展：支持 @SpringBootTest / @Transactional / @ActiveProfiles
    testImplementation("io.kotest:kotest-extensions-spring:6.2.4")
}
