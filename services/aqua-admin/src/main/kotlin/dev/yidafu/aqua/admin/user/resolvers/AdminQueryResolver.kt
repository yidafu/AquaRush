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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.AdminVo
import dev.yidafu.aqua.common.graphql.generated.AdminVoPage
import dev.yidafu.aqua.common.graphql.generated.RegionVo
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 账号/地区查询 Resolver(管理后台)。
 *
 * 规则 10:账号列表改分页 + keyword 过滤,详情走 admin(id),不再把全表账号下发前端。
 */
@Controller
class AdminQueryResolver(
  private val adminRepository: AdminRepository,
  private val regionRepository: RegionRepository,
) {
  private val logger = LoggerFactory.getLogger(AdminQueryResolver::class.java)

  /**
   * 账号分页列表(管理员权限)。keyword 模糊匹配 username/realName/phone。
   * size 上限 100,防止调用方一次拉全表绕过规则 10。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_READ')")
  fun admins(
    @Argument keyword: String?,
    @Argument role: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
  ): AdminVoPage {
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
    val result =
      adminRepository.findAllAdmins(
        keyword = keyword,
        role = role?.let { runCatching { AdminRoleModel.fromValue(it) }.getOrNull() },
        pageable = pageable,
      )
    return AdminVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 账号详情(管理员权限):超管账号详情页按 id 查询,替代原先「拉全量 admins 再前端 find」。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ADMIN_USER_READ')")
  fun admin(
    @Argument id: Long,
  ): AdminVo? = adminRepository.findAdminById(id)?.toGraphQL()

  /**
   * 按父级查询地区(parentCode 为空 → 省级列表),供前端树形结构逐级懒加载。
   * 每次返回条数有界(省级 ~34 / 市级 ~20 / 区级 ~20),符合 AGENTS.md 规则 10。
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun regions(
    @Argument parentCode: String?,
  ): List<RegionVo> =
    try {
      (
        if (parentCode.isNullOrBlank()) {
          regionRepository.findRootRegions(1)
        } else {
          regionRepository.findByParentCodeOrderByCode(parentCode)
        }
      ).map { it.toGraphQL() }
    } catch (e: Exception) {
      logger.error("Error fetching regions for parentCode={}", parentCode, e)
      emptyList()
    }

  private companion object {
    const val MAX_PAGE_SIZE = 100
  }
}
