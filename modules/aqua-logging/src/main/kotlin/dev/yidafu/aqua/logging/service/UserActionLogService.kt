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

package dev.yidafu.aqua.logging.service

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 用户操作日志服务
 */
@Service
class UserActionLogService(
  private val userActionLogRepository: UserActionLogRepository,
) {
  private val logger = LoggerFactory.getLogger(UserActionLogService::class.java)

  /**
   * 保存用户操作日志
   */
  @Async
  fun saveUserActionLog(
    userId: String? = null,
    username: String? = null,
    actionType: String,
    target: String? = null,
    pageUrl: String? = null,
    elementId: String? = null,
    elementType: String? = null,
    elementText: String? = null,
    clientIp: String? = null,
    userAgent: String? = null,
    properties: String? = null,
  ) {
    try {
      val userActionLog =
        UserActionLogModel(
          userId = userId,
          username = username,
          actionType = actionType,
          target = target,
          pageUrl = pageUrl,
          elementId = elementId,
          elementType = elementType,
          elementText = elementText,
          clientIp = clientIp,
          userAgent = userAgent,
          properties = properties,
          createdAt = LocalDateTime.now(),
        )
      userActionLogRepository.save(userActionLog)
      logger.debug("User action log saved: $actionType")
    } catch (e: Exception) {
      logger.error("Failed to save user action log: {}", e.message, e)
    }
  }

  /**
   * 分页查询用户操作日志
   */
  fun queryUserActionLogs(
    userId: String?,
    actionType: String?,
    username: String?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<UserActionLogModel> =
    userActionLogRepository.findByFilters(
      userId,
      actionType,
      username,
      startTime,
      endTime,
      pageable,
    )

  /**
   * 根据用户ID查询日志
   */
  fun findByUserId(userId: String): List<UserActionLogModel> = userActionLogRepository.findByUserId(userId)

  /**
   * 根据操作类型查询日志
   */
  fun findByActionType(actionType: String): List<UserActionLogModel> = userActionLogRepository.findByActionType(actionType)
}
