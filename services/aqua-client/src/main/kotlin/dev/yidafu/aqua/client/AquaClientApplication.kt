/*
 * AquaRush Client Service
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

package dev.yidafu.aqua.client

import dev.yidafu.aqua.common.cache.CacheAutoConfiguration
import dev.yidafu.aqua.storage.config.StorageProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
  scanBasePackages = [
    "dev.yidafu.aqua",
  ],
  exclude = [CacheAutoConfiguration::class],
)
@EnableScheduling
@EnableConfigurationProperties(StorageProperties::class)
@EntityScan(
  basePackages = [
    "dev.yidafu.aqua",
  ],
)
class AquaClientApplication

fun main(args: Array<String>) {
  runApplication<AquaClientApplication>(*args)
}
