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

package dev.yidafu.aqua.logging.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.logging.domain.BusinessLogModel
import dev.yidafu.aqua.logging.service.BusinessLogService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 业务日志查询控制器
 * 提供业务日志的查询接口
 */
@RestController
@RequestMapping("/api/logs/business")
class BizLogController(
  private val businessLogService: BusinessLogService,
) {
  /**
   * 分页查询业务日志
   */
  @GetMapping
  fun queryBusinessLogs(
    @RequestParam(required = false) level: String?,
    @RequestParam(required = false) loggerName: String?,
    @RequestParam(required = false) userId: Long?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
  ): ApiResponse<Page<BusinessLogModel>> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    val result =
      businessLogService.queryBusinessLogs(
        level,
        loggerName,
        userId,
        startTime,
        endTime,
        pageable,
      )
    return ApiResponse.success(result)
  }

  /**
   * 根据关联ID查询业务日志
   */
  @GetMapping("/correlation/{correlationId}")
  fun getBusinessLogsByCorrelationId(
    @PathVariable correlationId: String,
  ): List<BusinessLogModel> = businessLogService.findByCorrelationId(correlationId)
}
