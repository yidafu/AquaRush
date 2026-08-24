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

package dev.yidafu.aqua.order.service

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.config.ReceiptAutoConfirmProperties
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 自动确认收货兜底扫描(R-ORD-11 用户确认收货与自动签收):
 * 定时扫表自动签收「待确认收货」超期订单——送达 N 天(系统配置 auto_confirm_receipt_days,默认 7)
 * 未确认即自动置「已完成」并按自动确认时间计入营收。
 * 仅 admin 服务运行([AdminService],client 不扫描),间隔默认 5 分钟(可配 `aqua.order.receipt-confirm.scan-interval-ms`);
 * 分页游标分批扫描(禁止全表拉取,CLAUDE.md 规则 13);
 * 与用户确认并发由数据库乐观锁 CAS 认领(confirmReceiptAtomically)保证幂等与互斥,重复处理安全。
 */
@Component
@AdminService
class ReceiptAutoConfirmScheduler(
  private val orderRepository: OrderRepository,
  private val orderMutationService: OrderMutationService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
  private val properties: ReceiptAutoConfirmProperties,
) {
  private val logger = LoggerFactory.getLogger(ReceiptAutoConfirmScheduler::class.java)

  @Scheduled(
    fixedDelayString = "\${aqua.order.receipt-confirm.scan-interval-ms:300000}",
    initialDelayString = "\${aqua.order.receipt-confirm.scan-initial-delay-ms:60000}",
  )
  fun autoConfirmExpiredReceipts() {
    val autoConfirmDays =
      systemConfigQueryApiService.getLongValue(SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS, DEFAULT_AUTO_CONFIRM_DAYS)
    if (autoConfirmDays <= 0) {
      // 配置异常防御:自动确认天数非法时不扫描,避免误签收
      logger.warn("自动确认收货天数配置非法({}),跳过本次扫描", autoConfirmDays)
      return
    }

    val deadline = LocalDateTime.now().minusDays(autoConfirmDays)
    var pageNumber = 0
    while (true) {
      val pageable = PageRequest.of(pageNumber, properties.scanBatchSize, Sort.by(Sort.Direction.ASC, "id"))
      val page =
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(
          OrderModelStatus.AWAITING_CONFIRMATION,
          deadline,
          pageable,
        )
      page.content.forEach { order ->
        try {
          orderMutationService.autoConfirmReceipt(order.id!!)
        } catch (e: Exception) {
          logger.error("自动确认收货失败: 订单 {}", order.id, e)
        }
      }
      if (!page.hasNext()) {
        break
      }
      pageNumber++
    }
  }

  companion object {
    /** 自动确认收货默认天数(与 Liquibase 种子 auto_confirm_receipt_days=7 一致,配置缺失时兜底) */
    const val DEFAULT_AUTO_CONFIRM_DAYS = 7L
  }
}
