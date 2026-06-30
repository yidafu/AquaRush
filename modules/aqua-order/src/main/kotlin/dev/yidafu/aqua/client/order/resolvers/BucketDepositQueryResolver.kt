/**
 * AquaRush Client BucketDeposit Query Resolver
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

package dev.yidafu.aqua.client.order.resolvers

import dev.yidafu.aqua.api.service.BucketDepositService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@ClientService
@Controller("clientBucketDepositQueryResolver")
class BucketDepositQueryResolver(
  private val bucketDepositService: BucketDepositService,
) {
  /**
   * 获取当前用户的押桶记录列表
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myBucketDeposits(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<BucketDepositModel> = bucketDepositService.getUserBucketDeposits(userPrincipal.id)

  /**
   * 获取当前用户的押桶记录列表（分页）
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myBucketDepositsPaged(
    @Argument page: Int = 0,
    @Argument size: Int = 20,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Page<BucketDepositModel> = bucketDepositService.getUserBucketDeposits(userPrincipal.id, page, size)

  /**
   * 获取当前用户的有效押桶数量
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myActiveBucketCount(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Int = bucketDepositService.getUserActiveBucketCount(userPrincipal.id)

  /**
   * 获取押桶金额配置
   */
  @QueryMapping
  fun bucketDepositAmount(): Long = bucketDepositService.getBucketDepositAmount()

  /**
   * 根据ID获取押桶记录
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun bucketDeposit(
    @Argument depositId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): BucketDepositModel? {
    val deposit = bucketDepositService.getBucketDepositById(depositId)
    // 确保只能查看自己的押桶记录
    return if (deposit != null && deposit.userId == userPrincipal.id) {
      deposit
    } else {
      null
    }
  }
}
