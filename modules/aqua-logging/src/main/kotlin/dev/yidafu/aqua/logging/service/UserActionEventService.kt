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

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.DomainEvent
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.controller.UserActionController.UserActionLogRequest
import dev.yidafu.aqua.logging.util.UserActionLogger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 用户操作事件处理服务
 * 支持异步批量处理用户操作事件，提高系统性能
 */
@Service
@ConditionalOnProperty(prefix = "aqua.logging.userAction", name = ["asyncProcessing"], matchIfMissing = true)
class UserActionEventService(
  private val loggingProperties: LoggingProperties,
  private val userActionLogger: UserActionLogger,
) {
  private val logger = LoggerFactory.getLogger(UserActionEventService::class.java)
  private val objectMapper = jacksonObjectMapper()
  private val eventQueue = ConcurrentLinkedQueue<UserActionLogRequest>()
  private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  // 用户操作事件存储（在生产环境中应使用数据库）
  private val eventStore = mutableMapOf<String, DomainEvent>()

  init {
    // 启动定时任务，定期处理队列中的事件
    startEventProcessor()
  }

  /**
   * 异步处理用户操作日志
   * 将用户操作事件添加到队列中，等待批量处理
   */
  fun processUserActionAsync(request: UserActionLogRequest): Boolean {
    return try {
      eventQueue.offer(request)
      logger.debug("User action added to processing queue: ${request.actionType}")
      true
    } catch (e: Exception) {
      logger.error("Failed to queue user action: ${request.actionType}", e)
      // 降级处理：直接记录日志
      processUserActionSync(request)
      true
    }
  }

  /**
   * 批量处理用户操作日志
   */
  fun processUserActionsBatch(requests: List<UserActionLogRequest>): List<Boolean> {
    return try {
      val results = mutableListOf<Boolean>()
      val batchSize = loggingProperties.userAction.batchSize

      requests.chunked(batchSize).forEach { batch ->
        val batchResults = batch.map { processUserActionSync(it) }
        results.addAll(batchResults)

        logger.info("Processed batch of ${batch.size} user actions, " +
            "success: ${batchResults.count { it }}")
      }

      results
    } catch (e: Exception) {
      logger.error("Failed to process batch user actions", e)
      requests.map { false }
    }
  }

  /**
   * 同步处理用户操作日志
   * 直接记录到日志文件，用于关键操作或降级场景
   */
  @Transactional
  fun processUserActionSync(request: UserActionLogRequest): Boolean {
    return try {
      when (request.actionType.uppercase()) {
        "PAGE_VIEW" -> {
          val pageTitle = request.properties["pageTitle"] as? String
          val referrer = request.properties["referrer"] as? String
          val additionalData = request.properties.filterKeys { it != "pageTitle" && it != "referrer" }

          userActionLogger.logPageView(
            pageUrl = request.target,
            pageTitle = pageTitle,
            referrer = referrer,
            additionalData = additionalData
          )
        }

        "CLICK" -> {
          val coordinates = request.coordinates?.let {
            UserActionLogger.Coordinates(
              screenX = it.screenX,
              screenY = it.screenY,
              pageX = it.pageX,
              pageY = it.pageY
            )
          }

          userActionLogger.logClick(
            elementId = request.target,
            elementType = request.properties["elementType"] as? String ?: "button",
            elementText = request.properties["elementText"] as? String,
            coordinates = coordinates,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("elementType", "elementText")
            }
          )
        }

        "FORM_SUBMIT" -> {
          val success = request.properties["success"] as? Boolean ?: true
          val formFields = (request.properties["fields"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
          val formType = request.properties["formType"] as? String ?: "unknown"

          userActionLogger.logFormSubmit(
            formId = request.target,
            formType = formType,
            success = success,
            formFields = formFields,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("success", "fields", "formType")
            }
          )
        }

        "SEARCH" -> {
          val searchQuery = request.properties["searchQuery"] as? String ?: ""
          val searchType = request.properties["searchType"] as? String ?: "general"
          val resultCount = request.properties["resultCount"] as? Int
          val searchTime = request.properties["searchTime"] as? Long

          userActionLogger.logSearch(
            searchQuery = searchQuery,
            searchType = searchType,
            resultCount = resultCount,
            searchTime = searchTime,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("searchQuery", "searchType", "resultCount", "searchTime")
            }
          )
        }

        "SHARE" -> {
          val shareType = request.properties["shareType"] as? String ?: "unknown"
          val shareContent = request.properties["shareContent"] as? String

          userActionLogger.logShare(
            shareTarget = request.target,
            shareType = shareType,
            shareContent = shareContent,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("shareType", "shareContent")
            }
          )
        }

        "SCROLL" -> {
          val scrollDirection = request.properties["scrollDirection"] as? String ?: "down"
          val scrollDistance = request.properties["scrollDistance"] as? Int ?: 0
          val scrollTop = request.properties["scrollTop"] as? Int ?: 0
          val documentHeight = request.properties["documentHeight"] as? Int ?: 0

          userActionLogger.logScroll(
            scrollDirection = scrollDirection,
            scrollDistance = scrollDistance,
            scrollTop = scrollTop,
            documentHeight = documentHeight,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("scrollDirection", "scrollDistance", "scrollTop", "documentHeight")
            }
          )
        }

        "FILE_OPERATION" -> {
          val operation = request.properties["operation"] as? String ?: "unknown"
          val fileName = request.properties["fileName"] as? String ?: "unknown"
          val fileSize = request.properties["fileSize"] as? Long
          val fileType = request.properties["fileType"] as? String
          val success = request.properties["success"] as? Boolean ?: true

          userActionLogger.logFileOperation(
            operation = operation,
            fileName = fileName,
            fileSize = fileSize,
            fileType = fileType,
            success = success,
            additionalData = request.properties.filterKeys { key ->
              key !in setOf("operation", "fileName", "fileSize", "fileType", "success")
            }
          )
        }

        else -> {
          // 自定义操作类型
          userActionLogger.logCustomAction(
            actionType = request.actionType,
            target = request.target,
            properties = request.properties
          )
        }
      }

      true
    } catch (e: Exception) {
      logger.error("Failed to process user action: ${request.actionType}", e)
      false
    }
  }

  /**
   * 创建用户操作域事件
   * 用于消息队列集成
   */
  private fun createUserActionEvent(request: UserActionLogRequest): DomainEvent {
    val eventData = mapOf(
      "userId" to request.userId,
      "username" to request.username,
      "actionType" to request.actionType,
      "target" to request.target,
      "coordinates" to request.coordinates,
      "properties" to request.properties,
      "timestamp" to request.timestamp,
      "eventId" to UUID.randomUUID().toString()
    )

    return DomainEvent(
      id = DefaultIdGenerator().generate(),
      eventType = "USER_ACTION_${request.actionType.uppercase()}",
      payload = objectMapper.writeValueAsString(eventData),
    )
  }

  /**
   * 启动事件处理器
   * 定期从队列中取出事件进行批量处理
   */
  private fun startEventProcessor() {
    val flushIntervalMs = loggingProperties.userAction.flushInterval
    val batchSize = loggingProperties.userAction.batchSize

    scheduler.scheduleAtFixedRate({
      try {
        val eventsToProcess = mutableListOf<UserActionLogRequest>()

        // 从队列中取出事件
        repeat(batchSize) {
          val event = eventQueue.poll()
          if (event != null) {
            eventsToProcess.add(event)
          }
        }

        if (eventsToProcess.isNotEmpty()) {
          logger.info("Processing ${eventsToProcess.size} user actions from queue")

          // 批量处理事件
          processUserActionsBatch(eventsToProcess)

          // 创建域事件用于消息队列（如果启用）
          eventsToProcess.forEach { request ->
            try {
              val domainEvent = createUserActionEvent(request)
              eventStore[domainEvent.id.toString()] = domainEvent

              // 这里可以调用消息队列发布服务
              // eventPublishService.publishDomainEvent(...)

            } catch (e: Exception) {
              logger.error("Failed to create domain event for user action", e)
            }
          }
        }
      } catch (e: Exception) {
        logger.error("Error in event processor", e)
      }
    }, 0, flushIntervalMs, TimeUnit.MILLISECONDS)
  }

  /**
   * 强制刷新队列
   * 立即处理队列中的所有事件
   */
  fun flushQueue() {
    val remainingEvents = mutableListOf<UserActionLogRequest>()
    while (eventQueue.isNotEmpty()) {
      eventQueue.poll()?.let { remainingEvents.add(it) }
    }

    if (remainingEvents.isNotEmpty()) {
      logger.info("Force flushing ${remainingEvents.size} user actions from queue")
      processUserActionsBatch(remainingEvents)
    }
  }

  /**
   * 获取队列状态
   */
  fun getQueueStatus(): Map<String, Any> {
    return mapOf(
      "queueSize" to eventQueue.size,
      "batchSize" to loggingProperties.userAction.batchSize,
      "flushIntervalMs" to loggingProperties.userAction.flushInterval,
      "eventStoreSize" to eventStore.size,
      "asyncProcessing" to loggingProperties.userAction.asyncProcessing
    )
  }

  /**
   * 清理过期事件
   * 在生产环境中应使用数据库清理逻辑
   */
  fun cleanupExpiredEvents() {
    val retentionDays = loggingProperties.userAction.retentionDays
    val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

    val expiredEvents = eventStore.filter { (_, event) ->
      // 这里需要从payload中解析timestamp，简化处理
      true // 在实际实现中需要解析payload
    }

    expiredEvents.keys.forEach { eventStore.remove(it) }

    if (expiredEvents.isNotEmpty()) {
      logger.info("Cleaned up ${expiredEvents.size} expired user action events")
    }
  }

  /**
   * 关闭服务
   */
  fun shutdown() {
    try {
      logger.info("Shutting down UserActionEventService")
      flushQueue()
      scheduler.shutdown()

      if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
        scheduler.shutdownNow()
      }

      logger.info("UserActionEventService shutdown completed")
    } catch (e: Exception) {
      logger.error("Error during UserActionEventService shutdown", e)
    }
  }

  companion object {
    @JvmStatic
    val instance: UserActionEventService by lazy {
      UserActionEventService(LoggingProperties(), UserActionLogger.instance)
    }
  }
}
