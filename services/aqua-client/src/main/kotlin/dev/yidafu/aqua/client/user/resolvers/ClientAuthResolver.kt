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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.graphql.generated.UserInfoVo
import dev.yidafu.aqua.common.security.UserPrincipal
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 客户端用户认证 Resolver(用户端 schema:me 查询 / updateProfile 变更):
 * - me:当前登录用户信息(R-AUTH-02,仅本人)
 * - updateProfile:更新昵称/手机号/头像,返回更新后用户信息
 */
@ClientService
@Controller
class ClientAuthResolver(
  private val userService: UserService,
) {
  private val logger = LoggerFactory.getLogger(ClientAuthResolver::class.java)

  /**
   * 获取当前登录用户信息
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun me(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): UserInfoVo {
    val user =
      userService.findById(userPrincipal.id)
        ?: throw IllegalArgumentException("用户不存在")
    return user.toUserInfo()
  }

  /**
   * 更新当前登录用户资料(昵称/手机号/头像,仅本人,F-USR-003)
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun updateProfile(
    @Argument @Valid input: UpdateProfileInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): UserInfoVo {
    logger.info(
      "updateProfile for user: {}, fields: nickname={}, phone={}, avatar={}",
      userPrincipal.id,
      input.nickname != null,
      input.phone != null,
      input.avatar != null,
    )
    val updatedUser = userService.updateUserInfo(userPrincipal.id, input.nickname, input.phone, input.avatar)
    return updatedUser.toUserInfo()
  }

  /**
   * 领域实体 → GraphQL UserInfoVo(纯函数直构,手写转换不依赖编译期生成 mapper)
   */
  private fun UserModel.toUserInfo(): UserInfoVo =
    UserInfoVo(
      id = id ?: 0L,
      wechatOpenId = wechatOpenId,
      nickname = nickname,
      phone = phone,
      avatarUrl = avatarUrl,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
}
