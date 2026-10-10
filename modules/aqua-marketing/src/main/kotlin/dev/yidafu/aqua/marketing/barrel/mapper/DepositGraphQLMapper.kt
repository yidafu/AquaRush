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

package dev.yidafu.aqua.marketing.barrel.mapper

import dev.yidafu.aqua.api.dto.BarrelAccount
import dev.yidafu.aqua.api.dto.BarrelAccountSpec
import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.graphql.generated.BarrelAccountSpecVo
import dev.yidafu.aqua.common.graphql.generated.BarrelAccountVo
import dev.yidafu.aqua.common.graphql.generated.BarrelLedgerRowVo
import dev.yidafu.aqua.common.graphql.generated.DepositRecordVo
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationVo
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus as DomainDepositRefundStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus as DomainDepositStatus
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationStatus as GeneratedDepositRefundStatus
import dev.yidafu.aqua.common.graphql.generated.DepositStatus as GeneratedDepositStatus

/**
 * 押金域模型 → GraphQL VO(后台与用户端共用;状态枚举同名互转,金额 Long 分直传 R-MON-01)。
 */

fun DepositRecordModel.toGraphQL(): DepositRecordVo =
  DepositRecordVo(
    id = id ?: 0L,
    orderId = orderId,
    specification = specification,
    unitDepositCents = unitDepositCents,
    bucketCount = bucketCount,
    depositAmountCents = depositAmountCents,
    status = status.toGraphQL(),
    collectedAt = collectedAt,
    refundedAt = refundedAt,
    refundRemark = refundRemark,
    createdAt = createdAt,
  )

fun DepositRefundApplicationModel.toGraphQL(): DepositRefundApplicationVo =
  DepositRefundApplicationVo(
    id = id ?: 0L,
    specification = specification,
    bucketCount = bucketCount,
    depositCents = depositCents,
    receiverName = receiverName,
    receiverPhone = receiverPhone,
    receiverDetail = receiverDetail,
    remark = remark,
    status = status.toGraphQL(),
    rejectReason = rejectReason,
    recoveredAt = recoveredAt,
    refundedAt = refundedAt,
    rejectedAt = rejectedAt,
    createdAt = createdAt,
  )

fun BarrelAccountSpec.toGraphQL(): BarrelAccountSpecVo =
  BarrelAccountSpecVo(
    specification = specification,
    heldBucketCount = heldBucketCount,
    owedBucketCount = owedBucketCount,
    depositHeldCents = depositHeldCents,
  )

fun BarrelAccount.toGraphQL(): BarrelAccountVo =
  BarrelAccountVo(
    specs = specs.map { it.toGraphQL() },
    depositHeldCents = depositHeldCents,
    owedBucketCount = owedBucketCount,
    records = records.content.map { it.toGraphQL() },
    recordTotal = records.totalElements.toInt(),
  )

fun BarrelLedgerRow.toGraphQL(): BarrelLedgerRowVo =
  BarrelLedgerRowVo(
    userId = userId,
    nickname = nickname,
    phone = phone,
    heldBucketCount = heldBucketCount.toInt(),
    depositHeldCents = depositHeldCents,
    depositRefundedCents = depositRefundedCents,
    deliveredBucketCount = deliveredBucketCount.toInt(),
    recoveredBucketCount = recoveredBucketCount.toInt(),
    owedBucketCount = owedBucketCount.toInt(),
  )

fun DomainDepositStatus.toGraphQL(): GeneratedDepositStatus =
  when (this) {
    DomainDepositStatus.PENDING -> GeneratedDepositStatus.PENDING
    DomainDepositStatus.HELD -> GeneratedDepositStatus.HELD
    DomainDepositStatus.REFUNDED -> GeneratedDepositStatus.REFUNDED
  }

fun GeneratedDepositStatus.toDomain(): DomainDepositStatus =
  when (this) {
    GeneratedDepositStatus.PENDING -> DomainDepositStatus.PENDING
    GeneratedDepositStatus.HELD -> DomainDepositStatus.HELD
    GeneratedDepositStatus.REFUNDED -> DomainDepositStatus.REFUNDED
  }

fun DomainDepositRefundStatus.toGraphQL(): GeneratedDepositRefundStatus =
  when (this) {
    DomainDepositRefundStatus.PENDING -> GeneratedDepositRefundStatus.PENDING
    DomainDepositRefundStatus.RECOVERED -> GeneratedDepositRefundStatus.RECOVERED
    DomainDepositRefundStatus.REFUNDED -> GeneratedDepositRefundStatus.REFUNDED
    DomainDepositRefundStatus.REJECTED -> GeneratedDepositRefundStatus.REJECTED
  }

fun GeneratedDepositRefundStatus.toDomain(): DomainDepositRefundStatus =
  when (this) {
    GeneratedDepositRefundStatus.PENDING -> DomainDepositRefundStatus.PENDING
    GeneratedDepositRefundStatus.RECOVERED -> DomainDepositRefundStatus.RECOVERED
    GeneratedDepositRefundStatus.REFUNDED -> DomainDepositRefundStatus.REFUNDED
    GeneratedDepositRefundStatus.REJECTED -> DomainDepositRefundStatus.REJECTED
  }
