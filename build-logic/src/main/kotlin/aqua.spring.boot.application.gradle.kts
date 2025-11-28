plugins {
    id("aqua.kotlin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Configure bootJar task
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
    // Set main class if not automatically detected
    mainClass.set("dev.yidafu.aqua.AquaRushApplicationKt")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
}
