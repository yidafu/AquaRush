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

package dev.yidafu.aqua.client.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["dev.yidafu.aqua"])
@EnableJpaAuditing
@EnableTransactionManagement
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
class JpaConfig
