/*
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
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

package dev.yidafu.aqua.common.id.config

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.id.IdGenerator
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for Snowflake ID generator
 */
@ConfigurationProperties(prefix = "aqua.id.snowflake")
data class SnowflakeProperties(
  /** Machine ID for Snowflake generator (0-1023) */
  var machineId: Long = 1L,
)

/**
 * Spring Boot auto-configuration for Snowflake ID generator
 */
@Configuration
@EnableConfigurationProperties(SnowflakeProperties::class)
class SnowflakeConfig {
  @Bean
  @ConditionalOnMissingBean
  fun snowflakeIdGenerator(snowflakeProperties: SnowflakeProperties): SnowflakeIdGenerator =
    SnowflakeIdGenerator(snowflakeProperties.machineId).also {
      SnowflakeIdGenerator.initialize(snowflakeProperties.machineId)
    }

  @Bean
  @ConditionalOnMissingBean
  fun idGenerator(): IdGenerator = DefaultIdGenerator()
}

/**
 * Class-based configuration for modules that need ID generation
 */
@Configuration
class IdConfig(
  private val idGenerator: IdGenerator,
) {
  fun generateId(): Long = idGenerator.generate()
}
