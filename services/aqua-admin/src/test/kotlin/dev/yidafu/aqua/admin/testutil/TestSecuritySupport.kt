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

package dev.yidafu.aqua.admin.testutil

import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.test.context.TestSecurityContextHolder

/*
 * Kotest 测试安全上下文辅助函数。
 *
 * 原 JUnit 写法在测试方法上用 @WithMockUserPrincipal / @WithMockUser 注解由
 * WithSecurityContextTestExecutionListener 注入 SecurityContext；Kotest 的
 * SpringExtension 使用“伪方法”承载 Spring 生命周期，方法级注解无法被识别，
 * 因此在测试体开头显式调用以下函数设置安全上下文。
 *
 * 必须通过 TestSecurityContextHolder.setContext 写入：springSecurity() 挂载的
 * testSecurityContext() 后置处理器使用 TestSecurityContextHolderStrategyAdapter
 * 读取上下文（优先其自身 ThreadLocal），与 @WithMockUser 的注入路径完全一致。
 */

/** 模拟 @WithMockUserPrincipal（管理员 UserPrincipal，自动附加 ROLE_ADMIN） */
fun setMockUserPrincipal(
  id: Long = 1L,
  username: String = "admin",
  authorities: List<String> = emptyList(),
) {
  val granted = (authorities + "ROLE_ADMIN").map { SimpleGrantedAuthority(it) }
  val principal =
    UserPrincipal(
      id = id,
      _username = username,
      userType = "ADMIN",
      _authorities = granted,
    )
  val context = SecurityContextHolder.createEmptyContext()
  context.authentication = UsernamePasswordAuthenticationToken(principal, null, granted)
  TestSecurityContextHolder.setContext(context)
}

/** 模拟 @WithMockUser（Spring Security User；无 authorities 时默认 ROLE_USER） */
fun setMockUser(
  username: String = "user",
  password: String = "password",
  authorities: List<String> = emptyList(),
) {
  val granted =
    if (authorities.isEmpty()) {
      listOf(SimpleGrantedAuthority("ROLE_USER"))
    } else {
      authorities.map { SimpleGrantedAuthority(it) }
    }
  val principal = User(username, password, granted)
  val context = SecurityContextHolder.createEmptyContext()
  context.authentication = UsernamePasswordAuthenticationToken.authenticated(principal, password, granted)
  TestSecurityContextHolder.setContext(context)
}
