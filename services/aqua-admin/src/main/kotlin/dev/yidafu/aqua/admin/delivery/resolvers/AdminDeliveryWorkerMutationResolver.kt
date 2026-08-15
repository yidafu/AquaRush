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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.util.GraphQLSecurityContext
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import dev.yidafu.aqua.common.annotation.AdminService as AdminServiceAnnotation

/**
 * 管理端配送员变更解析器
 * 提供配送员管理的变更功能，仅管理员可访问
 *
 * 送水员档案并入 admins 单表后(PRD/07-user.md §F-USR-021),创建/更新送水员
 * 即创建/更新 role=DELIVERY_WORKER 的账号行,用户名自动为 `delivery_{账号ID}`。
 */
@AdminServiceAnnotation
@Controller
class AdminDeliveryWorkerMutationResolver(
  private val adminService: AdminService,
) {
  private val logger = LoggerFactory.getLogger(AdminDeliveryWorkerMutationResolver::class.java)

  /**
   * 创建新的配送员（管理员功能）
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_WRITE')")
  @Transactional
  fun createDeliveryWorker(
    @Argument input: CreateDeliveryWorkerInput,
  ): DeliveryWorker {
    try {
      // 验证输入
      validateCreateDeliveryWorkerInput(input)

      // 单表落库:role=DELIVERY_WORKER 账号行,username 自动生成 delivery_{账号ID};
      // wechatOpenId 不再落库(送水工端独立 App 账号密码登录)
      val admin =
        adminService.createAdmin(
          username = null,
          password = input.password,
          realName = input.name,
          phone = input.phone,
          role = AdminRoleModel.DELIVERY_WORKER,
          avatarUrl = input.avatarUrl,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = input.isAvailable ?: true,
          coordinates = input.coordinates,
          currentLocation = input.currentLocation,
          rating = input.rating,
          earningCents = input.earningCents,
          accountStatus = input.accountStatus ?: AdminAccountStatus.ACTIVE,
        )

      logger.info("Successfully created delivery worker: ${admin.id} - ${admin.username}")
      return DeliveryWorkerMapper.map(admin)
    } catch (e: Exception) {
      logger.error("Failed to create delivery worker", e)
      throw BadRequestException("创建送水工失败: ${e.message}")
    }
  }

  /**
   * 更新现有配送员（管理员功能）
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_WRITE')")
  @Transactional
  fun updateDeliveryWorker(
    @Argument workerId: Long,
    @Argument input: UpdateDeliveryWorkerInput,
  ): DeliveryWorker {
    try {
      // 获取现有账号(送水员档案并入 admins 单表后,workerId 即账号 id)
      val existingWorker =
        adminService.findById(workerId)
          ?: throw NotFoundException("送水工不存在: $workerId")

      // 验证输入
      validateUpdateDeliveryWorkerInput(input, existingWorker)

      val updated =
        adminService.updateAdmin(
          id = workerId,
          realName = input.name,
          phone = input.phone,
          role = null,
          avatarUrl = input.avatarUrl,
          onlineStatus = input.status,
          isAvailable = input.isAvailable,
          coordinates = input.coordinates,
          currentLocation = input.currentLocation,
          rating = input.rating,
          earningCents = input.earningCents,
          accountStatus = input.accountStatus,
        )
      logger.info("Successfully updated delivery worker: ${updated.id} - ${updated.username}")
      return DeliveryWorkerMapper.map(updated)
    } catch (e: Exception) {
      logger.error("Failed to update delivery worker", e)
      throw BadRequestException("更新送水工失败: ${e.message}")
    }
  }

  /**
   * 删除送水员(G11 落地,软删除)
   *
   * 仅可删除 role=DELIVERY_WORKER 的账号行;不能删除当前登录账号。
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_DELETE')")
  @Transactional
  fun deleteDeliveryWorker(
    @Argument id: Long,
  ): Boolean {
    try {
      val currentUserId = GraphQLSecurityContext.getCurrentUserId()
      if (id == currentUserId) {
        throw BadRequestException("不能删除当前登录账号")
      }

      val target = adminService.findById(id)
      if (target == null) {
        return false
      }
      if (target.role != AdminRoleModel.DELIVERY_WORKER) {
        throw BadRequestException("该账号不是送水员，无法通过送水员管理删除")
      }

      val deleted = adminService.deleteById(id)
      logger.info("Delivery worker deleted: $id")
      return deleted
    } catch (e: Exception) {
      if (e is BadRequestException) {
        throw e
      }
      logger.error("Failed to delete delivery worker", e)
      throw BadRequestException("删除送水员失败: ${e.message}")
    }
  }

  /**
   * 重置送水员密码(G11 落地;newPassword 留空则重置为默认密码 123456)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_RESET_PASSWORD')")
  @Transactional
  fun resetDeliveryWorkerPassword(
    @Argument id: Long,
    @Argument newPassword: String?,
  ): Boolean {
    try {
      val target = adminService.findById(id)
      if (target == null) {
        throw BadRequestException("送水员不存在: $id")
      }
      if (target.role != AdminRoleModel.DELIVERY_WORKER) {
        throw BadRequestException("该账号不是送水员，无法重置密码")
      }
      adminService.resetPassword(id, newPassword)
      logger.info("Delivery worker password reset: $id")
      return true
    } catch (e: Exception) {
      if (e is BadRequestException) {
        throw e
      }
      logger.error("Failed to reset delivery worker password", e)
      throw BadRequestException("重置送水员密码失败: ${e.message}")
    }
  }

  /**
   * 验证创建配送员输入
   */
  private fun validateCreateDeliveryWorkerInput(input: CreateDeliveryWorkerInput) {
    // wechatOpenId 现在是可选的，如果为空将自动生成

    if (input.name.isBlank()) {
      throw BadRequestException("姓名不能为空")
    }

    if (input.phone.isBlank()) {
      throw BadRequestException("手机号码不能为空")
    }

    if (!isValidPhoneNumber(input.phone)) {
      throw BadRequestException("手机号码格式不正确")
    }

    if (input.rating != null && input.rating!! < 0.0) {
      throw BadRequestException("评分不能为负数")
    }

    if (input.earningCents != null && input.earningCents!! < 0.0) {
      throw BadRequestException("收入不能为负数")
    }
  }

  /**
   * 验证更新配送员输入
   */
  private fun validateUpdateDeliveryWorkerInput(
    input: UpdateDeliveryWorkerInput,
    existingWorker: AdminModel,
  ) {
    // 验证wechatOpenId（如果提供）
    input.wechatOpenId?.let {
      if (it.isBlank()) {
        throw BadRequestException("微信OpenID不能为空")
      }
    }

    // 验证姓名（如果提供）
    input.name?.let {
      if (it.isBlank()) {
        throw BadRequestException("姓名不能为空")
      }
    }

    // 验证电话（如果提供）
    input.phone?.let {
      if (it.isBlank()) {
        throw BadRequestException("手机号码不能为空")
      }

      if (!isValidPhoneNumber(it)) {
        throw BadRequestException("手机号码格式不正确")
      }
    }

    // 验证评分（如果提供）
    input.rating?.let {
      if (it < 0.0) {
        throw BadRequestException("评分不能为负数")
      }
    }

    // 验证收入（如果提供）
    input.earningCents?.let {
      if (it < 0.0) {
        throw BadRequestException("收入不能为负数")
      }
    }
  }

  /**
   * 验证手机号码格式（简单验证）
   */
  private fun isValidPhoneNumber(phone: String): Boolean = phone.matches(Regex("^1[3-9]\\d{9}$"))

  companion object {
    /**
     * 配送员操作输入类型
     */
    data class CreateDeliveryWorkerInput(
      val wechatOpenId: String?,
      val name: String,
      val phone: String,
      val password: String?,
      val avatarUrl: String?,
      val coordinates: String?,
      val currentLocation: String?,
      val rating: Double?,
      val earningCents: Long?,
      val isAvailable: Boolean? = true,
      val accountStatus: AdminAccountStatus? = null,
    )

    data class UpdateDeliveryWorkerInput(
      val wechatOpenId: String?,
      val name: String?,
      val phone: String?,
      val avatarUrl: String?,
      val coordinates: String?,
      val currentLocation: String?,
      val rating: Double?,
      val earningCents: Long?,
      val isAvailable: Boolean?,
      val status: DeliverWorkerModelStatus?,
      val accountStatus: AdminAccountStatus?,
    )
  }
}
