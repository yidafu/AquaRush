/**
 * AquaRush AdminVo UserVo Mutation Resolver
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

import dev.yidafu.aqua.admin.user.dto.CreateAdminRequest
import dev.yidafu.aqua.admin.user.dto.UpdateAdminRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.AdminVo
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.CreateUserInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import dev.yidafu.aqua.common.graphql.generated.UpdateUserInput
import dev.yidafu.aqua.common.graphql.generated.UserVo
import dev.yidafu.aqua.common.graphql.util.GraphQLSecurityContext
import dev.yidafu.aqua.user.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.annotation.AdminService as AdminServiceAnnotation

/**
 * 管理端后台账号与用户变更解析器
 *
 * 管理员:创建/更新/删除/重置密码/停用(F-USR-020,G11 落地)
 * 送水员:删除/重置密码经 AdminDeliveryWorkerMutationResolver(F-USR-021,G11 落地)
 * 用户:创建/更新/删除(F-USR-022,G11 落地)
 */
@AdminServiceAnnotation
@Controller
class AdminUserMutationResolver(
  private val adminService: AdminService,
  private val userService: UserService,
) {
  private val logger = LoggerFactory.getLogger(AdminUserMutationResolver::class.java)

  /**
   * 创建管理员用户
   */
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_WRITE')")
  @MutationMapping
  fun createAdmin(
    @Argument input: CreateAdminInput,
  ): AdminVo {
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
          avatarUrl = request.avatarUrl,
          onlineStatus = request.onlineStatus,
          isAvailable = request.isAvailable,
          accountStatus = request.accountStatus,
        )

      logger.info("Successfully created admin user through resolver: ${createdAdmin.id}")
      return createdAdmin.toGraphQL()
    } catch (e: Exception) {
      logger.error("Failed to create admin user", e)
      throw BadRequestException("创建管理员失败: ${e.message}")
    }
  }

  /**
   * 更新管理员用户
   */
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_WRITE')")
  @MutationMapping
  fun updateAdmin(
    @Argument id: Long,
    @Argument input: UpdateAdminInput,
  ): AdminVo {
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
          avatarUrl = request.avatarUrl,
          onlineStatus = request.onlineStatus,
          isAvailable = request.isAvailable,
          accountStatus = request.accountStatus,
        )

      logger.info("Successfully updated admin user through resolver: ${updatedAdmin.id}")
      return updatedAdmin.toGraphQL()
    } catch (e: Exception) {
      logger.error("Failed to update admin user", e)
      throw BadRequestException("更新管理员失败: ${e.message}")
    }
  }

  /**
   * 删除管理员(G11 落地)
   *
   * 保护:不能删除当前登录账号;不能删除最后一个超级管理员
   */
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_WRITE')")
  @MutationMapping
  fun deleteAdmin(
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
      if (target.role == AdminRoleModel.SUPER_ADMIN && adminService.countByRole(AdminRoleModel.SUPER_ADMIN) <= 1) {
        throw BadRequestException("至少保留一个超级管理员账号")
      }

      val deleted = adminService.deleteById(id)
      logger.info("AdminVo deleted: $id")
      return deleted
    } catch (e: Exception) {
      if (e is BadRequestException) {
        throw e
      }
      logger.error("Failed to delete admin user", e)
      throw BadRequestException("删除管理员失败: ${e.message}")
    }
  }

  /**
   * 重置管理员密码(G11 落地;newPassword 留空则重置为默认密码 123456)
   */
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_WRITE')")
  @MutationMapping
  fun resetAdminPassword(
    @Argument id: Long,
    @Argument newPassword: String?,
  ): Boolean {
    try {
      adminService.resetPassword(id, newPassword)
      logger.info("AdminVo password reset: $id")
      return true
    } catch (e: Exception) {
      if (e is BadRequestException) {
        throw e
      }
      logger.error("Failed to reset admin password", e)
      throw BadRequestException("重置管理员密码失败: ${e.message}")
    }
  }

  /**
   * 后台创建用户(F-USR-022,G11 落地;wechatOpenId 由后端自动生成占位)
   */
  @PreAuthorize("hasAuthority('PERMISSION_USER_WRITE')")
  @MutationMapping
  fun createUser(
    @Argument input: CreateUserInput,
  ): UserVo {
    try {
      val created =
        userService.createUserByAdmin(
          nickname = input.nickname,
          phone = input.phone,
          avatarUrl = input.avatarUrl,
        )
      return created.toGraphQL()
    } catch (e: Exception) {
      logger.error("Failed to create user", e)
      throw BadRequestException("创建用户失败: ${e.message}")
    }
  }

  /**
   * 后台编辑用户(F-USR-022)
   */
  @PreAuthorize("hasAuthority('PERMISSION_USER_WRITE')")
  @MutationMapping
  fun updateUser(
    @Argument id: Long,
    @Argument input: UpdateUserInput,
  ): UserVo {
    try {
      val updated =
        userService.updateUserByAdmin(
          id = id,
          nickname = input.nickname,
          phone = input.phone,
          avatarUrl = input.avatarUrl,
          status = input.status,
        )
      return updated.toGraphQL()
    } catch (e: Exception) {
      logger.error("Failed to update user", e)
      throw BadRequestException("更新用户失败: ${e.message}")
    }
  }

  /**
   * 后台删除用户(F-USR-022,软删除)
   */
  @PreAuthorize("hasAuthority('PERMISSION_USER_DELETE')")
  @MutationMapping
  fun deleteUser(
    @Argument id: Long,
  ): Boolean {
    try {
      val deleted = userService.deleteUserById(id)
      logger.info("UserVo deleted: $id")
      return deleted
    } catch (e: Exception) {
      logger.error("Failed to delete user", e)
      throw BadRequestException("删除用户失败: ${e.message}")
    }
  }

  private fun validateCreateAdminInput(input: CreateAdminInput) {
    // 送水员角色(DELIVERY_WORKER)可不传用户名,后端自动生成 delivery_{账号ID}(F-USR-021)
    val username = input.username
    if (username == null) {
      if (input.role != dev.yidafu.aqua.common.graphql.generated.AdminRole.DELIVERY_WORKER) {
        throw BadRequestException("用户名不能为空")
      }
      return
    }
    if (username.isBlank()) {
      throw BadRequestException("用户名不能为空")
    }
    if (username.length !in 3..50) {
      throw BadRequestException("用户名长度应在3-50个字符之间")
    }
    if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
      throw BadRequestException("用户名只能包含字母、数字和下划线")
    }
  }
}
