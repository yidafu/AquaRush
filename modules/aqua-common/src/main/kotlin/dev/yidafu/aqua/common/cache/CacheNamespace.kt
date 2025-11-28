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

package dev.yidafu.aqua.common.cache

/**
 * 缓存命名空间接口
 * 支持多个命名空间隔离不同模块的缓存数据
 */
interface CacheNamespace {
  /**
   * 获取命名空间名称
   */
  val name: String

  /**
   * 获取指定命名空间的缓存管理器
   *
   * @param namespace 命名空间名称
   * @return 缓存管理器实例
   */
  fun getCache(namespace: String): CacheManager

  /**
   * 移除指定命名空间的所有缓存
   *
   * @param namespace 命名空间名称
   */
  fun clearNamespace(namespace: String)

  /**
   * 获取所有命名空间名称
   *
   * @return 命名空间名称集合
   */
  fun getAllNamespaces(): Set<String>

  /**
   * 检查命名空间是否存在
   *
   * @param namespace 命名空间名称
   * @return true如果存在，false如果不存在
   */
  fun hasNamespace(namespace: String): Boolean

  /**
   * 创建新的命名空间缓存
   *
   * @param namespace 命名空间名称
   * @param config 缓存配置
   * @return 缓存管理器实例
   */
  fun createNamespace(
    namespace: String,
    config: CacheConfig = CacheConfig(),
  ): CacheManager

  /**
   * 删除命名空间及其所有缓存
   *
   * @param namespace 命名空间名称
   * @return true如果成功删除，false如果命名空间不存在
   */
  fun deleteNamespace(namespace: String): Boolean
}
