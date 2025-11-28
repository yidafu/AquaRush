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

package dev.yidafu.aqua

import dev.yidafu.aqua.common.cache.CacheAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
  scanBasePackages = [
    "dev.yidafu.aqua.common",
    "dev.yidafu.aqua.api",
    "dev.yidafu.aqua.config",
    "dev.yidafu.aqua.delivery",
    "dev.yidafu.aqua.notice",
    "dev.yidafu.aqua.order",
    "dev.yidafu.aqua.payment",
    "dev.yidafu.aqua.product",
    "dev.yidafu.aqua.reconciliation",
    "dev.yidafu.aqua.review",
    "dev.yidafu.aqua.statistics",
    "dev.yidafu.aqua.storage",
    "dev.yidafu.aqua.user",
  ],
  exclude = [CacheAutoConfiguration::class],
)
@EnableScheduling
class AquaRushApplication

fun main(args: Array<String>) {
  runApplication<AquaRushApplication>(*args)
}
