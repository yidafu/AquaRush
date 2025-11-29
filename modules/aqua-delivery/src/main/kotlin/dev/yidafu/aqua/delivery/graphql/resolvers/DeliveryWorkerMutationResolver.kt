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

package dev.yidafu.aqua.delivery.graphql.resolvers

import dev.yidafu.aqua.common.domain.model.DeliveryWorker
import dev.yidafu.aqua.common.domain.model.WorkerStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Controller
class DeliveryWorkerMutationResolver(
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val deliveryService: DeliveryService,
) {
  private val logger = LoggerFactory.getLogger(DeliveryWorkerMutationResolver::class.java)

  /**
   * Create a new delivery worker
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun createDeliveryWorker(
    @Argument input: CreateDeliveryWorkerInput
  ): DeliveryWorker {
    try {
      // Validate input
      validateCreateDeliveryWorkerInput(input)

      // Check if wechatOpenId or phone already exists
      if (deliveryWorkerRepository.existsByWechatOpenId(input.wechatOpenId)) {
        throw BadRequestException("该微信OpenID已存在: ${input.wechatOpenId}")
      }

      if (deliveryWorkerRepository.existsByPhone(input.phone)) {
        throw BadRequestException("该手机号码已存在: ${input.phone}")
      }

      // Create new delivery worker
      val worker = DeliveryWorker(
        userId = 0L, // Will be updated when WeChat integration is available
        wechatOpenId = input.wechatOpenId,
        name = input.name,
        phone = input.phone,
        avatarUrl = input.avatarUrl,
        status = WorkerStatus.OFFLINE, // Default to offline
        coordinates = input.coordinates,
        currentLocation = input.currentLocation,
        rating = input.rating,
        earning = input.earning,
        isAvailable = input.isAvailable ?: true,
      )

      val savedWorker = deliveryWorkerRepository.save(worker)
      logger.info("Successfully created delivery worker: ${savedWorker.id} - ${savedWorker.name}")
      return savedWorker
    } catch (e: Exception) {
      logger.error("Failed to create delivery worker", e)
      throw BadRequestException("创建送水工失败: ${e.message}")
    }
  }

  /**
   * Update an existing delivery worker
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun updateDeliveryWorker(
    @Argument workerId: Long,
    @Argument input: UpdateDeliveryWorkerInput
  ): DeliveryWorker {
    try {
      // Get existing worker
      val existingWorker = deliveryWorkerRepository.findById(workerId)
        .orElseThrow { NotFoundException("送水工不存在: $workerId") }

      // Validate input
      validateUpdateDeliveryWorkerInput(input, existingWorker)

      // Check if wechatOpenId or phone already exists (excluding current worker)
      input.wechatOpenId?.let { newWechatOpenId ->
        if (newWechatOpenId != existingWorker.wechatOpenId &&
            deliveryWorkerRepository.existsByWechatOpenId(newWechatOpenId)) {
          throw BadRequestException("该微信OpenID已存在: $newWechatOpenId")
        }
      }

      input.phone?.let { newPhone ->
        if (newPhone != existingWorker.phone &&
            deliveryWorkerRepository.existsByPhone(newPhone)) {
          throw BadRequestException("该手机号码已存在: $newPhone")
        }
      }

      // Update worker fields
      input.wechatOpenId?.let { existingWorker.wechatOpenId = it }
      input.name?.let { existingWorker.name = it }
      input.phone?.let { existingWorker.phone = it }
      input.avatarUrl?.let { existingWorker.avatarUrl = it }
      input.coordinates?.let { existingWorker.coordinates = it }
      input.currentLocation?.let { existingWorker.currentLocation = it }
      input.rating?.let { existingWorker.rating = it }
      input.earning?.let { existingWorker.earning = it }
      input.isAvailable?.let { existingWorker.isAvailable = it }

      val updatedWorker = deliveryWorkerRepository.save(existingWorker)
      logger.info("Successfully updated delivery worker: ${updatedWorker.id} - ${updatedWorker.name}")
      return updatedWorker
    } catch (e: Exception) {
      logger.error("Failed to update delivery worker", e)
      throw BadRequestException("更新送水工失败: ${e.message}")
    }
  }

  /**
   * Delete a delivery worker
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun deleteDeliveryWorker(
    @Argument workerId: Long
  ): Boolean {
    return try {
      if (!deliveryWorkerRepository.existsById(workerId)) {
        throw NotFoundException("送水工不存在: $workerId")
      }

      // Check if worker has active deliveries
      val activeDeliveries = deliveryService.getWorkerActiveTasks(workerId)
      if (activeDeliveries.isNotEmpty()) {
        throw BadRequestException("该送水工还有进行中的配送任务，无法删除")
      }

      deliveryWorkerRepository.deleteById(workerId)
      logger.info("Successfully deleted delivery worker: $workerId")
      true
    } catch (e: Exception) {
      logger.error("Failed to delete delivery worker", e)
      throw BadRequestException("删除送水工失败: ${e.message}")
    }
  }

  /**
   * Validate create delivery worker input
   */
  private fun validateCreateDeliveryWorkerInput(input: CreateDeliveryWorkerInput) {
    if (input.wechatOpenId.isBlank()) {
      throw BadRequestException("微信OpenID不能为空")
    }

    if (input.name.isBlank()) {
      throw BadRequestException("姓名不能为空")
    }

    if (input.phone.isBlank()) {
      throw BadRequestException("手机号码不能为空")
    }

    if (!isValidPhoneNumber(input.phone)) {
      throw BadRequestException("手机号码格式不正确")
    }

    if (input.rating != null && input.rating!! < BigDecimal.ZERO) {
      throw BadRequestException("评分不能为负数")
    }

    if (input.earning != null && input.earning!! < BigDecimal.ZERO) {
      throw BadRequestException("收入不能为负数")
    }
  }

  /**
   * Validate update delivery worker input
   */
  private fun validateUpdateDeliveryWorkerInput(
    input: UpdateDeliveryWorkerInput,
    existingWorker: DeliveryWorker
  ) {
    // Validate wechatOpenId if provided
    input.wechatOpenId?.let {
      if (it.isBlank()) {
        throw BadRequestException("微信OpenID不能为空")
      }
    }

    // Validate name if provided
    input.name?.let {
      if (it.isBlank()) {
        throw BadRequestException("姓名不能为空")
      }
    }

    // Validate phone if provided
    input.phone?.let {
      if (it.isBlank()) {
        throw BadRequestException("手机号码不能为空")
      }

      if (!isValidPhoneNumber(it)) {
        throw BadRequestException("手机号码格式不正确")
      }
    }

    // Validate rating if provided
    input.rating?.let {
      if (it < BigDecimal.ZERO) {
        throw BadRequestException("评分不能为负数")
      }
    }

    // Validate earning if provided
    input.earning?.let {
      if (it < BigDecimal.ZERO) {
        throw BadRequestException("收入不能为负数")
      }
    }
  }

  /**
   * Validate phone number format (simple validation)
   */
  private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^1[3-9]\\d{9}$"))
  }
}

/**
 * Input types for delivery worker operations
 */
data class CreateDeliveryWorkerInput(
  val wechatOpenId: String,
  val name: String,
  val phone: String,
  val avatarUrl: String?,
  val coordinates: String?,
  val currentLocation: String?,
  val rating: BigDecimal?,
  val earning: BigDecimal?,
  val isAvailable: Boolean? = true
)

data class UpdateDeliveryWorkerInput(
  val wechatOpenId: String?,
  val name: String?,
  val phone: String?,
  val avatarUrl: String?,
  val coordinates: String?,
  val currentLocation: String?,
  val rating: BigDecimal?,
  val earning: BigDecimal?,
  val isAvailable: Boolean?
)