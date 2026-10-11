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

package dev.yidafu.aqua.subscription.service.impl

import dev.yidafu.aqua.api.dto.CreateSubscriptionRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.SubscriptionMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import dev.yidafu.aqua.subscription.service.SubscriptionCycleCalculator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * 周期订购计划管理实现(F-ORD-026 后台入口,R-ORD-13):
 * 创建时校验地址归属/商品在售/周期参数并计算首次执行日期;
 * 暂停/恢复/终止按状态机流转,终止不可恢复。
 */
@Service
class SubscriptionMutationServiceImpl(
  private val subscriptionRepository: SubscriptionRepository,
  private val addressQueryApiService: AddressQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
  private val adminService: AdminService,
) : SubscriptionMutationService {
  private val logger = LoggerFactory.getLogger(SubscriptionMutationServiceImpl::class.java)

  @Transactional
  override fun create(
    adminId: Long,
    request: CreateSubscriptionRequest,
  ): SubscriptionModel {
    val (weekdays, monthDay) = validateCycleParams(request)
    val startDate = parseDate(request.startDate, "生效日期")
    val endDate = request.endDate?.let { parseDate(it, "终止日期") }
    if (endDate != null && endDate.isBefore(startDate)) {
      throw BadRequestException("终止日期不能早于生效日期")
    }
    request.maxPeriods?.let {
      if (it < 1) throw BadRequestException("期次上限必须大于0")
    }

    // 地址归属校验:计划必须绑定该用户本人的收货地址(R-ORD-13 快照口径,生成订单时取当时值)
    val address =
      addressQueryApiService.findById(request.addressId)
        ?: throw NotFoundException("收货地址不存在: ${request.addressId}")
    if (address.userId != request.userId) {
      throw BadRequestException("收货地址与用户不匹配")
    }

    // 商品在售校验(库存与配送范围在到期生成订单时校验,失败不静默、次日重试)
    val productIds = request.items.map { it.productId }
    if (productIds.distinct().size != productIds.size) {
      throw BadRequestException("计划商品明细存在重复商品")
    }
    request.items.forEach { item ->
      val product =
        productQueryApiService.findById(item.productId)
          ?: throw NotFoundException("商品不存在: ${item.productId}")
      if (product.status != ProductModelStatus.ONLINE) {
        throw BadRequestException("商品「${product.name}」已下架,请选择在售商品")
      }
    }

    // 绑定送水工校验(可空;送水工并入 admins 单表)
    request.workerId?.let { workerId ->
      adminService.findById(workerId)
        ?: throw BadRequestException("绑定的送水工不存在: $workerId")
    }

    val firstRun =
      SubscriptionCycleCalculator.firstRun(
        startDate = startDate,
        cycleType = request.cycleType,
        weekdays = weekdays,
        monthDay = monthDay,
      )
    if (endDate != null && firstRun.isAfter(endDate)) {
      throw BadRequestException("生效日期与终止条件下没有可执行的配送日")
    }

    val plan =
      SubscriptionModel(
        userId = request.userId,
        addressId = request.addressId,
        workerId = request.workerId,
        cycleType = request.cycleType,
        weekdays = weekdays?.let { SubscriptionCycleCalculator.formatWeekdays(it) },
        monthDay = monthDay,
        timeSlot = request.timeSlot,
        startDate = startDate,
        endDate = endDate,
        maxPeriods = request.maxPeriods,
        executedPeriods = 0,
        nextRunDate = firstRun,
        status = SubscriptionStatus.ACTIVE,
        remark = request.remark,
        createdBy = adminId,
      )
    val saved = subscriptionRepository.save(plan)
    subscriptionRepository.saveItems(
      request.items.map { item ->
        SubscriptionItemModel(
          subscriptionId = saved.id!!,
          productId = item.productId,
          quantity = item.quantity,
        )
      },
    )
    logger.info(
      "创建周期订购计划: id={}, userId={}, cycle={}, nextRunDate={}",
      saved.id,
      saved.userId,
      saved.cycleType,
      saved.nextRunDate,
    )
    return saved
  }

  @Transactional
  override fun pause(id: Long): SubscriptionModel {
    val plan = subscriptionRepository.findById(id) ?: throw NotFoundException("周期订购计划不存在: $id")
    if (plan.status != SubscriptionStatus.ACTIVE) {
      throw BadRequestException("仅生效中的计划可暂停")
    }
    plan.status = SubscriptionStatus.PAUSED
    plan.updatedAt = LocalDateTime.now()
    return subscriptionRepository.save(plan)
  }

  @Transactional
  override fun resume(id: Long): SubscriptionModel {
    val plan = subscriptionRepository.findById(id) ?: throw NotFoundException("周期订购计划不存在: $id")
    if (plan.status != SubscriptionStatus.PAUSED) {
      throw BadRequestException("仅已暂停的计划可恢复")
    }
    // 暂停期间错过的期次不补送:恢复时若下次配送日已过,按今天重算下一生效日
    val today = LocalDate.now()
    if (plan.nextRunDate.isBefore(today)) {
      plan.nextRunDate =
        SubscriptionCycleCalculator.firstRun(
          startDate = today,
          cycleType = plan.cycleType,
          weekdays = SubscriptionCycleCalculator.parseWeekdays(plan.weekdays),
          monthDay = plan.monthDay,
        )
    }
    plan.status = SubscriptionStatus.ACTIVE
    plan.updatedAt = LocalDateTime.now()
    return subscriptionRepository.save(plan)
  }

  @Transactional
  override fun terminate(id: Long): SubscriptionModel {
    val plan = subscriptionRepository.findById(id) ?: throw NotFoundException("周期订购计划不存在: $id")
    if (plan.status == SubscriptionStatus.TERMINATED) {
      throw BadRequestException("计划已终止")
    }
    plan.status = SubscriptionStatus.TERMINATED
    plan.updatedAt = LocalDateTime.now()
    return subscriptionRepository.save(plan)
  }

  /** 周期参数校验并归一化:返回 (weekdays, monthDay),不适用侧为 null */
  private fun validateCycleParams(request: CreateSubscriptionRequest): Pair<List<Int>?, Int?> =
    when (request.cycleType) {
      SubscriptionCycleType.WEEKLY,
      SubscriptionCycleType.BIWEEKLY,
      -> {
        val weekdays =
          request.weekdays
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        if (weekdays.isEmpty() || weekdays.any { it !in 1..7 }) {
          throw BadRequestException("请正确设置配送星期(1=周一..7=周日)")
        }
        weekdays to null
      }
      SubscriptionCycleType.MONTHLY -> {
        val monthDay = request.monthDay
        if (monthDay == null || monthDay !in 1..31) {
          throw BadRequestException("请正确设置每月配送日期(1-31)")
        }
        null to monthDay
      }
    }

  private fun parseDate(
    value: String,
    label: String,
  ): LocalDate =
    try {
      LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
      throw BadRequestException("${label}格式应为yyyy-MM-dd: $value")
    }
}
