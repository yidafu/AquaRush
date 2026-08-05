/**
 * AquaRush Admin User Mutation Resolver
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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.admin.user.resolvers.dto.CreateAdminRequest
import dev.yidafu.aqua.admin.user.resolvers.dto.UpdateAdminRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.Admin
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import dev.yidafu.aqua.user.mapper.AdminMapper
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.annotation.AdminService as AdminServiceAnnotation

/**
 * 管理端管理员用户变更解析器
 * 提供管理员用户的创建、更新、删除功能
 */
@AdminServiceAnnotation
@Controller
class AdminUserMutationResolver(
  private val adminService: AdminService,
) {
  private val logger = LoggerFactory.getLogger(AdminUserMutationResolver::class.java)

  /**
   * 创建管理员用户
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun createAdmin(
    @Argument input: CreateAdminInput,
  ): Admin {
    try {
      // 验证输入
      validateCreateAdminInput(input)

      // 转换输入为请求对象
      val request = CreateAdminRequest.fromInput(input)

      // 调用服务创建管理员
      val createdAdmin =
        adminService.createAdmin(
          username = request.username,
          password = request.password,
          realName = request.realName,
          phone = request.phone,
          role = request.role,
        )

      logger.info("Successfully created admin user through resolver: ${createdAdmin.id}")
      return AdminMapper.map(createdAdmin)
    } catch (e: Exception) {
      logger.error("Failed to create admin user", e)
      throw BadRequestException("创建管理员失败: ${e.message}")
    }
  }

  /**
   * 更新管理员用户
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun updateAdmin(
    @Argument id: Long,
    @Argument input: UpdateAdminInput,
  ): Admin {
    try {
      // 转换输入为请求对象
      val request = UpdateAdminRequest.fromInput(input)

      // 调用服务更新管理员
      val updatedAdmin =
        adminService.updateAdmin(
          id = id,
          realName = request.realName,
          phone = request.phone,
          role = request.role,
        )

      logger.info("Successfully updated admin user through resolver: ${updatedAdmin.id}")
      return AdminMapper.map(updatedAdmin)
    } catch (e: Exception) {
      logger.error("Failed to update admin user", e)
      throw BadRequestException("更新管理员失败: ${e.message}")
    }
  }

  /**
   * 删除管理员用户
   */
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @MutationMapping
  fun deleteAdmin(
    @Argument id: Long,
  ): Boolean {
    try {
      // 调用服务删除管理员
      adminService.deleteAdmin(id)
      logger.info("Successfully deleted admin user through resolver: $id")
      return true
    } catch (e: Exception) {
      logger.error("Failed to delete admin user", e)
      throw BadRequestException("删除管理员失败: ${e.message}")
    }
  }

  private fun validateCreateAdminInput(input: CreateAdminInput) {
    if (input.username.isBlank()) {
      throw BadRequestException("用户名不能为空")
    }
    if (input.username.length !in 3..50) {
      throw BadRequestException("用户名长度应在3-50个字符之间")
    }
    if (!input.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
      throw BadRequestException("用户名只能包含字母、数字和下划线")
    }
  }
}
