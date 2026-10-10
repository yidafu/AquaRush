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

package dev.yidafu.aqua.client.barrel.resolvers

import dev.yidafu.aqua.api.dto.SubmitDepositRefundRequest
import dev.yidafu.aqua.api.service.deposit.DepositQueryService
import dev.yidafu.aqua.api.service.deposit.DepositRefundMutationService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.BarrelAccountVo
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationVo
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationVoPage
import dev.yidafu.aqua.common.graphql.generated.SubmitDepositRefundInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.barrel.mapper.toDomain
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端水桶账户与退押(F-PRO-010/R-PRO-04,押金模型回退 B2):
 * 桶账聚合 + 押金记录分页 + 退押申请提交/记录;本人数据隔离(R-AUTH-02)。
 */
@ClientService
@Controller
class ClientDepositResolver(
  private val depositQueryService: DepositQueryService,
  private val depositRefundMutationService: DepositRefundMutationService,
) {
  /** 水桶账户:按规格在租/欠桶/押金合计 + 押金记录分页 */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun barrelAccount(
    @Argument page: Int?,
    @Argument size: Int?,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): BarrelAccountVo =
    depositQueryService
      .barrelAccount(userId = principal.id, page = page ?: 0, size = size ?: 10)
      .toGraphQL()

  /** 退押申请记录(仅本人) */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun depositRefundApplications(
    @Argument status: DepositRefundApplicationStatus?,
    @Argument page: Int?,
    @Argument size: Int?,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): DepositRefundApplicationVoPage {
    val result =
      depositQueryService.listUserApplications(
        userId = principal.id,
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

  /** 提交退押申请(桶数 ≤ 在租押金桶数,服务端校验) */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun submitDepositRefund(
    @Argument @Valid input: SubmitDepositRefundInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): DepositRefundApplicationVo =
    depositRefundMutationService
      .submit(
        userId = principal.id,
        request =
          SubmitDepositRefundRequest(
            specification = input.specification,
            bucketCount = input.bucketCount,
            receiverName = input.receiverName,
            receiverPhone = input.receiverPhone,
            receiverDetail = input.receiverDetail,
            remark = input.remark,
          ),
      ).toGraphQL()
}
