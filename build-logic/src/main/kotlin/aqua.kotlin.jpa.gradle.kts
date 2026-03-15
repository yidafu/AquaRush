plugins {
  id("aqua.kotlin.spring")
  kotlin("plugin.jpa")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.postgresql:postgresql")
}
