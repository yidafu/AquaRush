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

package dev.yidafu.aqua.admin.logging.resolvers

import dev.yidafu.aqua.common.graphql.generated.BusinessLogListInput
import dev.yidafu.aqua.common.graphql.generated.BusinessLogVoPage
import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import dev.yidafu.aqua.logging.mapper.toGraphQL
import dev.yidafu.aqua.logging.service.BusinessLogService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 业务日志查询 Resolver(管理后台,PERMISSION_LOG_READ)
 * 与 PRD 09-platform-logs.md §F-LOG-003 保持一致:业务日志时间线展示,
 * 恒 createdAt 倒序,支持业务域(domain)/操作类型(operationType)维度筛选。
 */
@Controller
class AdminBusinessLogQueryResolver(
  private val businessLogService: BusinessLogService,
) {
  /**
   * 分页查询业务日志(恒 createdAt 倒序,PageInfoVo 分页)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_LOG_READ')")
  fun businessLogs(
    @Argument input: BusinessLogListInput?,
  ): BusinessLogVoPage {
    val page = businessLogService.queryBusinessLogs(input ?: BusinessLogListInput())
    return BusinessLogVoPage(
      list = page.content.map { it.toGraphQL() },
      pageInfo =
        PageInfoVo(
          total = page.totalElements.toInt(),
          pageSize = page.size,
          pageNum = page.number,
          hasNext = page.hasNext(),
          hasPrevious = page.hasPrevious(),
          totalPages = page.totalPages,
        ),
    )
  }
}
