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

import dev.yidafu.aqua.api.service.marketing.TicketTypeMutationService
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.CreateTicketTypeInput
import dev.yidafu.aqua.common.graphql.generated.UpdateTicketTypeInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketTypeNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.mapper.toDomain
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
  private val bizLogger: BizLogger,
) : TicketTypeMutationService {
  private val logger = LoggerFactory.getLogger(TicketTypeMutationServiceImpl::class.java)

  @Transactional
  override fun createTicketType(
    adminId: Long,
    adminName: String,
    input: CreateTicketTypeInput,
  ): TicketTypeModel {
    // GraphQL 枚举 → 领域枚举
    val ticketType = input.ticketType.toDomain()
    val validityType = input.validityType.toDomain()
    validateType(ticketType, input.productId, input.specification)
    validateValidity(validityType, input.validFrom, input.validTo, input.validDays)
    val ticketTypeModel =
      TicketTypeModel(
        name = input.name,
        ticketType = ticketType,
        productId = input.productId,
        specification = input.specification,
        packageCount = input.packageCount,
        packagePriceCents = input.packagePriceCents,
        giftCount = input.giftCount,
        validityType = validityType,
        validFrom = input.validFrom,
        validTo = input.validTo,
        validDays = input.validDays,
        perUserLimit = input.perUserLimit,
        status = TicketTypeStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = ticketTypeRepository.save(ticketTypeModel)
    logger.info("Admin {} created ticket type {}: {}", adminId, saved.id, saved.name)

    // 业务日志(F-LOG-003 MARKETING 域):票种创建
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "TICKET_TYPE_CREATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name, "ticketType" to saved.ticketType.name),
    )
    return saved
  }

  @Transactional
  override fun updateTicketType(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdateTicketTypeInput,
  ): TicketTypeModel {
    val ticketType = ticketTypeRepository.findById(id).orElseThrow { TicketTypeNotFoundException(id) }
    // GraphQL 枚举 → 领域枚举
    val kind = input.ticketType.toDomain()
    val validityType = input.validityType.toDomain()
    validateType(kind, input.productId, input.specification)
    validateValidity(validityType, input.validFrom, input.validTo, input.validDays)
    ticketType.name = input.name
    ticketType.ticketType = kind
    ticketType.productId = input.productId
    ticketType.specification = input.specification
    ticketType.packageCount = input.packageCount
    ticketType.packagePriceCents = input.packagePriceCents
    ticketType.giftCount = input.giftCount
    ticketType.validityType = validityType
    ticketType.validFrom = input.validFrom
    ticketType.validTo = input.validTo
    ticketType.validDays = input.validDays
    ticketType.perUserLimit = input.perUserLimit
    val saved = ticketTypeRepository.save(ticketType)
    logger.info("Admin {} updated ticket type {}", adminId, id)

    // 业务日志(F-LOG-003 MARKETING 域):票种更新
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "TICKET_TYPE_UPDATED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
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

    // 业务日志(F-LOG-003 MARKETING 域):票种停用
    bizLogger.logMarketingOperation(
      targetId = saved.id!!,
      operation = "TICKET_TYPE_DISABLED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("name" to saved.name),
    )
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
