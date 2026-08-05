/**
 * AquaRush Admin BucketDeposit Query Resolver
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

import dev.yidafu.aqua.api.service.order.BucketDepositService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import dev.yidafu.aqua.common.domain.model.BucketDepositStatus
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
@Controller("adminBucketDepositQueryResolver")
class AdminBucketDepositQueryResolver(
  private val bucketDepositService: BucketDepositService,
) {
  /**
   * 获取所有押桶记录（管理员）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun allBucketDeposits(
    @Argument status: BucketDepositStatus? = null,
    @Argument userId: Long? = null,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
  ): Page<BucketDepositModel> = bucketDepositService.getAllBucketDeposits(status, userId, page, size)

  /**
   * 根据ID获取押桶记录（管理员）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun bucketDeposit(
    @Argument depositId: Long,
  ): BucketDepositModel? = bucketDepositService.getBucketDepositById(depositId)

  /**
   * 获取押桶金额配置（管理员）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun bucketDepositAmount(): Long = bucketDepositService.getBucketDepositAmount()
}
