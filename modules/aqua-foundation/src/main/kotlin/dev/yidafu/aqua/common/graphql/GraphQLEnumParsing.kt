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

package dev.yidafu.aqua.common.graphql

import dev.yidafu.aqua.common.graphql.generated.BizLogLevel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus

/**
 * GraphQL 生成枚举的字符串解析入口(schema.kt 由 codegen 生成,不可修改,故以伴生扩展提供)。
 *
 * 与领域枚举的 `fromValue` 同语义:显式 `when` 穷举合法值,未匹配抛
 * [IllegalArgumentException](fail-fast,不静默吞数据)。
 */
fun RefundStatus.Companion.fromValue(value: String): RefundStatus =
  when (value) {
    "APPROVED" -> RefundStatus.APPROVED
    "COMPLETED" -> RefundStatus.COMPLETED
    "FAILED" -> RefundStatus.FAILED
    "PENDING" -> RefundStatus.PENDING
    "REJECTED" -> RefundStatus.REJECTED
    else -> throw IllegalArgumentException("Unknown RefundStatus value: '" + value + "'")
  }

fun RefundType.Companion.fromValue(value: String): RefundType =
  when (value) {
    "DEPOSIT_REFUND" -> RefundType.DEPOSIT_REFUND
    "ORDER_REFUND" -> RefundType.ORDER_REFUND
    else -> throw IllegalArgumentException("Unknown RefundType value: '" + value + "'")
  }

fun UserStatus.Companion.fromValue(value: String): UserStatus =
  when (value) {
    "ACTIVE" -> UserStatus.ACTIVE
    "DELETED" -> UserStatus.DELETED
    "INACTIVE" -> UserStatus.INACTIVE
    "ONLINE" -> UserStatus.ONLINE
    "SUSPENDED" -> UserStatus.SUSPENDED
    else -> throw IllegalArgumentException("Unknown UserStatus value: '" + value + "'")
  }

fun UserRole.Companion.fromValue(value: String): UserRole =
  when (value) {
    "ADMIN" -> UserRole.ADMIN
    "NONE" -> UserRole.NONE
    "USER" -> UserRole.USER
    "WORKER" -> UserRole.WORKER
    else -> throw IllegalArgumentException("Unknown UserRole value: '" + value + "'")
  }

fun BizLogLevel.Companion.fromValue(value: String): BizLogLevel =
  when (value) {
    "DEBUG" -> BizLogLevel.DEBUG
    "ERROR" -> BizLogLevel.ERROR
    "INFO" -> BizLogLevel.INFO
    "TRACE" -> BizLogLevel.TRACE
    "WARN" -> BizLogLevel.WARN
    else -> throw IllegalArgumentException("Unknown BizLogLevel value: '" + value + "'")
  }
