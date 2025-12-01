/*
 * AquaRush Admin Service
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

package dev.yidafu.aqua.admin

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.cache.CacheAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.persistence.autoconfigure.EntityScan

@EntityScan(basePackages = [
  "dev.yidafu.aqua.common.domain.model",
  "dev.yidafu.aqua.user.domain.model",
  "dev.yidafu.aqua.product.domain.model",
  "dev.yidafu.aqua.order.domain.model",
  "dev.yidafu.aqua.payment.domain.model",
  "dev.yidafu.aqua.delivery.domain.model",
  "dev.yidafu.aqua.review.domain.model",
  "dev.yidafu.aqua.notice.domain.model",
  "dev.yidafu.aqua.reconciliation.domain.model",
  "dev.yidafu.aqua.storage.domain.entity"
])
@AdminService
@SpringBootApplication(
  scanBasePackages = [
    "dev.yidafu.aqua.common",
    "dev.yidafu.aqua.api",
    "dev.yidafu.aqua.admin.config",
    "dev.yidafu.aqua.logging",
    "dev.yidafu.aqua.user",
    "dev.yidafu.aqua.product",
    "dev.yidafu.aqua.storage",
    "dev.yidafu.aqua.notice",
    "dev.yidafu.aqua.reconciliation",
    "dev.yidafu.aqua.delivery",
    "dev.yidafu.aqua.statistics",
    "dev.yidafu.aqua.review",
    "dev.yidafu.aqua.payment",
    "dev.yidafu.aqua.order",

    "dev.yidafu.aqua.admin.user.resolvers",
    "dev.yidafu.aqua.admin.product.resolvers",
    "dev.yidafu.aqua.admin.storage.resolvers",
    "dev.yidafu.aqua.admin.notice.resolvers",
    "dev.yidafu.aqua.admin.reconciliation.resolvers",
    "dev.yidafu.aqua.admin.delivery.resolvers",
    "dev.yidafu.aqua.admin.statistics.resolvers",
    "dev.yidafu.aqua.admin.review.resolvers",
    "dev.yidafu.aqua.admin.payment.resolvers",
    "dev.yidafu.aqua.admin.order.resolvers",
  ],
  exclude = [CacheAutoConfiguration::class],
)
@EnableScheduling
class AquaAdminApplication

fun main(args: Array<String>) {
  runApplication<AquaAdminApplication>(*args)
}
