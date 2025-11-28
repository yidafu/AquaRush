/*
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

package dev.yidafu.aqua.logging.context

/**
 * 用户上下文持有者，用于存储当前请求的用户信息
 */
object UserContextHolder {
  private val userContext = ThreadLocal<UserContext>()

  /**
   * 设置当前线程的用户上下文
   */
  fun setUserContext(context: UserContext) {
    userContext.set(context)
  }

  /**
   * 获取当前线程的用户上下文
   */
  fun getUserContext(): UserContext? = userContext.get()

  /**
   * 获取当前用户ID
   */
  fun getUserId(): String? = getUserContext()?.userId

  /**
   * 获取当前用户名
   */
  fun getUsername(): String? = getUserContext()?.username

  /**
   * 获取当前用户角色
   */
  fun getUserRole(): String? = getUserContext()?.role

  /**
   * 清除当前线程的用户上下文
   */
  fun clear() {
    userContext.remove()
  }

  /**
   * 检查当前线程是否有用户上下文
   */
  fun hasUserContext(): Boolean = userContext.get() != null
}

/**
 * 用户上下文数据类
 */
data class UserContext(
  val userId: String,
  val username: String,
  val role: String? = null,
  val tenantId: String? = null,
  val additionalInfo: Map<String, Any> = emptyMap(),
)
