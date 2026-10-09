/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
plugins {
  id("aqua.spring.boot.library")
  alias(libs.plugins.mappie)
  id("aqua.kotlin.exposed")
}

dependencies {
  implementation(project(":modules:aqua-foundation"))

  // Mappie
  implementation(libs.mappie.api)

  // Spring Boot(barrel 子域模块内解析器需 web/security/validation/actuator)
  implementation(libs.bundles.spring.boot.web)
  implementation(libs.bundles.spring.boot.security)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.actuator)

  // GraphQL(barrel 子域模块内解析器)
  implementation(libs.bundles.graphql)
  implementation(libs.spring.boot.starter.graphql)

  // Test
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.mockk.core)
  testImplementation(libs.springmockk)
  testImplementation("org.springframework.graphql:spring-graphql-test:2.0.0")
  // 仓储实现 H2 实现级测试(aqua.kotlin.exposed 不带 test 依赖)
  testImplementation(libs.h2)
}
