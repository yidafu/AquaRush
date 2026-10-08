plugins {
  id("aqua.kotlin.spring")
  kotlin("plugin.jpa")
  // kotlin("plugin.jpa") only enables the no-arg constructor for @Entity classes;
  // it does NOT enable all-open. We must explicitly apply all-open with the JPA
  // annotations so Hibernate can create lazy proxies for entity getters.
  kotlin("plugin.allopen")
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.postgresql:postgresql")
}
