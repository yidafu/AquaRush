/*
 * AquaRush Admin BucketDeposit Mutation Resolver
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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.BucketDepositService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
@Controller("adminBucketDepositMutationResolver")
class AdminBucketDepositMutationResolver(
  private val bucketDepositService: BucketDepositService,
) {
  /**
   * 退还押金（管理员）
   * 注意：实际管理后台通过登录获取管理员ID，这里简化处理
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun refundBucketDeposit(
    @Argument depositId: Long,
    @Argument remark: String? = null,
  ): BucketDepositModel {
    // 管理员ID从上下文获取，这里暂时使用固定值
    // 在实际实现中应该从 SecurityContext 获取
    val adminId = 1L
    return bucketDepositService.refundBucketDeposit(
      depositId = depositId,
      adminId = adminId,
      remark = remark,
    )
  }

  /**
   * 设置押桶金额（管理员）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun setBucketDepositAmount(
    @Argument amountCents: Long,
  ): Boolean =
    try {
      bucketDepositService.setBucketDepositAmount(amountCents)
      true
    } catch (e: Exception) {
      false
    }
}
