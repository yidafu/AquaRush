/**
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

package dev.yidafu.aqua.marketing.barrel.resolvers

import dev.yidafu.aqua.api.dto.UpsertDepositPriceRequest
import dev.yidafu.aqua.api.service.deposit.DepositPriceMutationService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.DepositPriceVo
import dev.yidafu.aqua.common.graphql.generated.UpsertDepositPriceInput
import dev.yidafu.aqua.marketing.barrel.mapper.toDomain
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端押金价目变更(R-MON-03 后台押金管理「价目」新增/编辑;
 * PERMISSION_PRODUCT_WRITE,设计02 §5.1 upsertDepositPrice)
 */
@Controller
@AdminService
class AdminDepositPriceMutationResolver(
  private val depositPriceMutationService: DepositPriceMutationService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDepositPriceMutationResolver::class.java)

  /** 押金价目新增/编辑(id 空=新增,规格唯一) */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun upsertDepositPrice(
    @Argument @Valid input: UpsertDepositPriceInput,
  ): DepositPriceVo {
    val saved =
      depositPriceMutationService.upsert(
        UpsertDepositPriceRequest(
          id = input.id,
          specification = input.specification,
          depositCents = input.depositCents,
          status = input.status?.toDomain() ?: dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus.ENABLED,
          sortOrder = input.sortOrder ?: 999,
        ),
      )
    logger.info("押金价目 upsert: id={}, specification={}", saved.id, saved.specification)
    return saved.toGraphQL()
  }
}
