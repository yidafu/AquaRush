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

package dev.yidafu.aqua.marketing.ticket.service.impl

import dev.yidafu.aqua.api.dto.TicketTypeCreateRequest
import dev.yidafu.aqua.api.dto.TicketTypeUpdateRequest
import dev.yidafu.aqua.api.service.marketing.TicketTypeMutationService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketTypeNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 桶水票票种变更服务实现(管理后台,F-MKT-007):
 * 指定商品票校验商品;通兑票校验规格;票种/套餐配置变更不影响已购票(快照)。
 */
@Service
class TicketTypeMutationServiceImpl(
  private val ticketTypeRepository: TicketTypeRepository,
) : TicketTypeMutationService {
  private val logger = LoggerFactory.getLogger(TicketTypeMutationServiceImpl::class.java)

  @Transactional
  override fun createTicketType(
    adminId: Long,
    adminName: String,
    request: TicketTypeCreateRequest,
  ): TicketTypeModel {
    validateType(request.ticketType, request.productId, request.specification)
    validateValidity(request.validityType, request.validFrom, request.validTo, request.validDays)
    val ticketType =
      TicketTypeModel(
        name = request.name,
        ticketType = request.ticketType,
        productId = request.productId,
        specification = request.specification,
        packageCount = request.packageCount,
        packagePriceCents = request.packagePriceCents,
        giftCount = request.giftCount,
        validityType = request.validityType,
        validFrom = request.validFrom,
        validTo = request.validTo,
        validDays = request.validDays,
        perUserLimit = request.perUserLimit,
        status = TicketTypeStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = ticketTypeRepository.save(ticketType)
    logger.info("Admin {} created ticket type {}: {}", adminId, saved.id, saved.name)
    return saved
  }

  @Transactional
  override fun updateTicketType(
    adminId: Long,
    adminName: String,
    id: Long,
    request: TicketTypeUpdateRequest,
  ): TicketTypeModel {
    val ticketType = ticketTypeRepository.findById(id).orElseThrow { TicketTypeNotFoundException(id) }
    validateType(request.ticketType, request.productId, request.specification)
    validateValidity(request.validityType, request.validFrom, request.validTo, request.validDays)
    ticketType.name = request.name
    ticketType.ticketType = request.ticketType
    ticketType.productId = request.productId
    ticketType.specification = request.specification
    ticketType.packageCount = request.packageCount
    ticketType.packagePriceCents = request.packagePriceCents
    ticketType.giftCount = request.giftCount
    ticketType.validityType = request.validityType
    ticketType.validFrom = request.validFrom
    ticketType.validTo = request.validTo
    ticketType.validDays = request.validDays
    ticketType.perUserLimit = request.perUserLimit
    val saved = ticketTypeRepository.save(ticketType)
    logger.info("Admin {} updated ticket type {}", adminId, id)
    return saved
  }

  @Transactional
  override fun disableTicketType(
    adminId: Long,
    adminName: String,
    id: Long,
  ): TicketTypeModel {
    val ticketType = ticketTypeRepository.findById(id).orElseThrow { TicketTypeNotFoundException(id) }
    ticketType.status = TicketTypeStatus.DISABLED
    val saved = ticketTypeRepository.save(ticketType)
    logger.info("Admin {} disabled ticket type {}", adminId, id)
    return saved
  }

  private fun validateType(
    ticketType: TicketTypeKind,
    productId: Long?,
    specification: String?,
  ) {
    when (ticketType) {
      TicketTypeKind.SPECIFIC_PRODUCT -> require(productId != null) { "指定商品票必须选择适用商品" }
      TicketTypeKind.UNIVERSAL -> require(!specification.isNullOrBlank()) { "通兑票必须填写适用规格" }
    }
  }

  private fun validateValidity(
    validityType: ValidityType,
    validFrom: LocalDateTime?,
    validTo: LocalDateTime?,
    validDays: Int?,
  ) {
    when (validityType) {
      ValidityType.FIXED_RANGE -> {
        require(validFrom != null && validTo != null) { "有效期不合法:固定区间必须填开始与结束时间" }
        require(validFrom.isBefore(validTo)) { "有效期不合法:开始时间必须早于结束时间" }
      }
      ValidityType.DAYS_AFTER_PURCHASE -> require(validDays != null && validDays > 0) { "有效期不合法:购票后N天必须为正数" }
      else -> Unit
    }
  }
}
