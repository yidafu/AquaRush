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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.isNotEmpty

/**
 * 管理端配送员变更解析器
 * 提供配送员管理的变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminDeliveryWorkerMutationResolver(
    private val deliveryWorkerRepository: DeliveryWorkerRepository,
    private val deliveryService: DeliveryService,
) {
    private val logger = LoggerFactory.getLogger(AdminDeliveryWorkerMutationResolver::class.java)

    /**
     * 创建新的配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun createDeliveryWorker(input: CreateDeliveryWorkerInput): DeliveryWorker {
        try {
            // 验证输入
            validateCreateDeliveryWorkerInput(input)

            // 检查wechatOpenId或电话是否已存在
            if (deliveryWorkerRepository.existsByWechatOpenId(input.wechatOpenId)) {
                throw BadRequestException("该微信OpenID已存在: ${input.wechatOpenId}")
            }

            if (deliveryWorkerRepository.existsByPhone(input.phone)) {
                throw BadRequestException("该手机号码已存在: ${input.phone}")
            }

            // 创建新配送员
            val worker = DeliveryWorkerModel(
                userId = 0L, // WeChat集成可用时将更新
                wechatOpenId = input.wechatOpenId,
                name = input.name,
                phone = input.phone,
                avatarUrl = input.avatarUrl,
                onlineStatus = DeliverWorkerStatus.OFFLINE, // 默认为离线
                coordinates = input.coordinates,
                currentLocation = input.currentLocation,
                rating = input.rating,
                earning = input.earning,
                isAvailable = input.isAvailable ?: true,
            )

            val savedWorker = deliveryWorkerRepository.save(worker)
            logger.info("Successfully created delivery worker: ${savedWorker.id} - ${savedWorker.name}")
            return savedWorker.let{ DeliveryWorkerMapper.map(it) }
        } catch (e: Exception) {
            logger.error("Failed to create delivery worker", e)
            throw BadRequestException("创建送水工失败: ${e.message}")
        }
    }

    /**
     * 更新现有配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun updateDeliveryWorker(workerId: Long, input: UpdateDeliveryWorkerInput): DeliveryWorker {
        try {
            // 获取现有配送员
            val existingWorker = deliveryWorkerRepository.findById(workerId)
                .orElseThrow { NotFoundException("送水工不存在: $workerId") }

            // 验证输入
            validateUpdateDeliveryWorkerInput(input, existingWorker)

            // 检查wechatOpenId或电话是否已存在（排除当前配送员）
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

            // 更新配送员字段
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
            return updatedWorker.let{ DeliveryWorkerMapper.map(it) }
        } catch (e: Exception) {
            logger.error("Failed to update delivery worker", e)
            throw BadRequestException("更新送水工失败: ${e.message}")
        }
    }

    /**
     * 删除配送员（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun deleteDeliveryWorker(workerId: Long): Boolean {
        return try {
            if (!deliveryWorkerRepository.existsById(workerId)) {
                throw NotFoundException("送水工不存在: $workerId")
            }

            // 检查配送员是否有活跃配送任务
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
     * 验证创建配送员输入
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

        if (input.rating != null && input.rating!! < 0.0) {
            throw BadRequestException("评分不能为负数")
        }

        if (input.earning != null && input.earning!! < 0.0) {
            throw BadRequestException("收入不能为负数")
        }
    }

    /**
     * 验证更新配送员输入
     */
    private fun validateUpdateDeliveryWorkerInput(
        input: UpdateDeliveryWorkerInput,
        existingWorker: DeliveryWorkerModel
    ) {
        // 验证wechatOpenId（如果提供）
        input.wechatOpenId?.let {
            if (it.isBlank()) {
                throw BadRequestException("微信OpenID不能为空")
            }
        }

        // 验证姓名（如果提供）
        input.name?.let {
            if (it.isBlank()) {
                throw BadRequestException("姓名不能为空")
            }
        }

        // 验证电话（如果提供）
        input.phone?.let {
            if (it.isBlank()) {
                throw BadRequestException("手机号码不能为空")
            }

            if (!isValidPhoneNumber(it)) {
                throw BadRequestException("手机号码格式不正确")
            }
        }

        // 验证评分（如果提供）
        input.rating?.let {
            if (it < 0.0) {
                throw BadRequestException("评分不能为负数")
            }
        }

        // 验证收入（如果提供）
        input.earning?.let {
            if (it < 0.0) {
                throw BadRequestException("收入不能为负数")
            }
        }
    }

    /**
     * 验证手机号码格式（简单验证）
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    companion object {
        /**
         * 配送员操作输入类型
         */
        data class CreateDeliveryWorkerInput(
            val wechatOpenId: String,
            val name: String,
            val phone: String,
            val avatarUrl: String?,
            val coordinates: String?,
            val currentLocation: String?,
            val rating: Double?,
            val earning: Double?,
            val isAvailable: Boolean? = true
        )

        data class UpdateDeliveryWorkerInput(
            val wechatOpenId: String?,
            val name: String?,
            val phone: String?,
            val avatarUrl: String?,
            val coordinates: String?,
            val currentLocation: String?,
            val rating: Double?,
            val earning: Double?,
            val isAvailable: Boolean?
        )
    }
}
