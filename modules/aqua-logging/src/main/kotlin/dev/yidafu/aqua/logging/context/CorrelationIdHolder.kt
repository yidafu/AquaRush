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

import java.util.UUID

/**
 * 关联ID持有者，用于在整个请求生命周期中维护关联ID
 */
object CorrelationIdHolder {
  private val correlationId = ThreadLocal<String>()

  /**
   * 设置当前线程的关联ID
   */
  fun setCorrelationId(id: String) {
    correlationId.set(id)
  }

  /**
   * 获取当前线程的关联ID
   */
  fun getCorrelationId(): String? = correlationId.get()

  /**
   * 生成新的关联ID并设置到当前线程
   */
  fun generateAndSet(): String {
    val newId = UUID.randomUUID().toString().replace("-", "")
    setCorrelationId(newId)
    return newId
  }

  /**
   * 清除当前线程的关联ID
   */
  fun clear() {
    correlationId.remove()
  }

  /**
   * 检查当前线程是否有关联ID
   */
  fun hasCorrelationId(): Boolean = correlationId.get() != null
}
