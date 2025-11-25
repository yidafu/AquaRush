plugins {
    id("aqua.kotlin.jpa")
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
}
