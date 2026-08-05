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

package dev.yidafu.aqua.admin.logging.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.api.dto.PageImpl
import dev.yidafu.aqua.logging.service.ApiLogService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * API日志查询控制器
 * 提供API日志的查询接口
 */
@RestController
@RequestMapping("/api/logs/api")
class ApiLogController(
  private val apiLogService: ApiLogService,
) {
  /**
   * 分页查询API日志
   */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  fun queryApiLogs(
    @RequestParam(required = false) requestType: RequestType?,
    @RequestParam(required = false) method: String?,
    @RequestParam(required = false) responseStatus: Int?,
    @RequestParam(required = false) userId: Long?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
  ): ApiResponse<PageImpl<ApiLogModel>> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

    val data =
      PageImpl.fromPage(
        apiLogService.queryApiLogs(
          requestType,
          method,
          responseStatus,
          userId,
          startTime,
          endTime,
          pageable,
        ),
      )
    return ApiResponse.success(data)
  }

  /**
   * 根据关联ID查询日志
   */
  @GetMapping("/correlation/{correlationId}")
  fun getApiLogsByCorrelationId(
    @PathVariable correlationId: String,
  ): ApiResponse<List<ApiLogModel>> = ApiResponse.success(apiLogService.findByCorrelationId(correlationId))
}
