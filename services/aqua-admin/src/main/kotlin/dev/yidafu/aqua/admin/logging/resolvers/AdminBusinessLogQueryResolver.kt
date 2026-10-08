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

import dev.yidafu.aqua.api.dto.BusinessLogListRequest
import dev.yidafu.aqua.common.graphql.generated.BusinessLogListInput
import dev.yidafu.aqua.common.graphql.generated.BusinessLogVoPage
import dev.yidafu.aqua.common.graphql.generated.PageInfo
import dev.yidafu.aqua.logging.mapper.BusinessLogListInputMapper
import dev.yidafu.aqua.logging.mapper.BusinessLogMapper
import dev.yidafu.aqua.logging.service.BusinessLogService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
   * 分页查询业务日志(恒 createdAt 倒序,PageInfo 分页)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_LOG_READ')")
  fun businessLogs(
    @Argument input: BusinessLogListInput?,
  ): BusinessLogVoPage {
    val request = input?.let { BusinessLogListInputMapper.map(it) } ?: BusinessLogListRequest()
    val pageable = PageRequest.of(request.page, request.size, Sort.by(Sort.Direction.DESC, "createdAt"))
    val page =
      businessLogService.queryBusinessLogs(
        level = request.level,
        loggerName = request.domain,
        operation = request.operationType,
        userId = request.userId,
        startTime = request.startTime,
        endTime = request.endTime,
        pageable = pageable,
      )
    return BusinessLogVoPage(
      list = page.content.map { BusinessLogMapper.map(it) },
      pageInfo =
        PageInfo(
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
