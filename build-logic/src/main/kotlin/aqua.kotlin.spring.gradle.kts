plugins {
    id("aqua.kotlin.common")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot BOM is now managed at root level via dependencyManagement
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.10")
}
