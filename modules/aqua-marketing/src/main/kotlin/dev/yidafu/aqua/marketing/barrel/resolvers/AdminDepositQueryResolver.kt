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

import dev.yidafu.aqua.api.service.deposit.DepositQueryService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.BarrelLedgerInput
import dev.yidafu.aqua.common.graphql.generated.BarrelLedgerVoPage
import dev.yidafu.aqua.common.graphql.generated.DepositRecordListInput
import dev.yidafu.aqua.common.graphql.generated.DepositRecordVoPage
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationVoPage
import dev.yidafu.aqua.marketing.barrel.mapper.toDomain
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端押金查询(R-MON-03 后台押金管理:押金记录 / 退押申请 / 桶账台账 F-PRO-012;
 * 全部强制分页,PERMISSION_PRODUCT_READ)。
 */
@Controller
@AdminService
class AdminDepositQueryResolver(
  private val depositQueryService: DepositQueryService,
) : BaseGraphQLResolver() {
  /** 押金记录分页(用户/状态/规格筛选) */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_READ')")
  fun depositRecords(
    @Argument input: DepositRecordListInput?,
  ): DepositRecordVoPage {
    val result =
      depositQueryService.searchDepositRecords(
        userId = input?.userId,
        status = input?.status?.toDomain(),
        specification = input?.specification,
        page = input?.page ?: 0,
        size = input?.size ?: 20,
      )
    return DepositRecordVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /** 退押申请分页(状态筛选;与用户端同签名) */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_READ')")
  fun depositRefundApplications(
    @Argument status: dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationStatus?,
    @Argument page: Int?,
    @Argument size: Int?,
  ): DepositRefundApplicationVoPage {
    val result =
      depositQueryService.listApplications(
        status = status?.toDomain(),
        page = page ?: 0,
        size = size ?: 20,
      )
    return DepositRefundApplicationVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /** 桶账台账(按客户实时聚合,关键词 + 仅看有欠桶,F-PRO-012) */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_READ')")
  fun barrelLedger(
    @Argument input: BarrelLedgerInput?,
  ): BarrelLedgerVoPage {
    val result =
      depositQueryService.barrelLedger(
        keyword = input?.keyword,
        onlyOwed = input?.onlyOwed ?: false,
        page = input?.page ?: 0,
        size = input?.size ?: 20,
      )
    return BarrelLedgerVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }
}
