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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

/**
 * 测试注解:构造以 UserPrincipal 为 principal 的认证上下文
 * 用于 @AuthenticationPrincipal principal: UserPrincipal 的 resolver 方法(@WithMockUser 的 principal 是 User,无法注入 UserPrincipal)。
 */
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory::class)
annotation class WithMockUserPrincipal(
  val id: Long = 1,
  val username: String = "admin",
  val authorities: Array<String> = [],
)

/**
 * 构造 SecurityContext:principal=UserPrincipal,authorities=指定权限 + ROLE_ADMIN
 */
class WithMockUserPrincipalSecurityContextFactory : WithSecurityContextFactory<WithMockUserPrincipal> {
  override fun createSecurityContext(annotation: WithMockUserPrincipal): SecurityContext {
    val authorities =
      (annotation.authorities.toList() + "ROLE_ADMIN").map { SimpleGrantedAuthority(it) }
    val principal =
      UserPrincipal(
        id = annotation.id,
        _username = annotation.username,
        userType = "ADMIN",
        _authorities = authorities,
      )
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
    return context
  }
}
