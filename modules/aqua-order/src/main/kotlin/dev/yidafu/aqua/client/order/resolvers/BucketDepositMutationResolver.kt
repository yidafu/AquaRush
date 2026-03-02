/*
 * AquaRush Client BucketDeposit Mutation Resolver
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
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@ClientService
@Controller("clientBucketDepositMutationResolver")
class BucketDepositMutationResolver(
  private val bucketDepositService: BucketDepositService,
) {
  /**
   * 创建押桶记录
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun createBucketDeposit(
    @Argument quantity: Int,
    @Argument openId: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): BucketDepositModel =
    bucketDepositService.createBucketDeposit(
      userId = userPrincipal.id,
      quantity = quantity,
      openId = openId,
    )

  /**
   * 处理押桶支付成功回调
   */
  @MutationMapping
  fun handleBucketDepositPaymentSuccess(
    @Argument depositId: Long,
    @Argument transactionId: String,
  ): Boolean =
    try {
      bucketDepositService.handlePaymentSuccess(depositId, transactionId)
      true
    } catch (e: Exception) {
      false
    }
}
