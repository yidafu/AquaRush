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

package dev.yidafu.aqua.client.delivery.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerMapper
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 客户端配送员变更解析器
 * 提供配送员自服务的变更功能，配送员只能修改自己的信息
 */
@ClientService
@Controller
class ClientDeliveryWorkerMutationResolver(
    private val deliveryWorkerRepository: DeliveryWorkerRepository
) {
    private val logger = LoggerFactory.getLogger(ClientDeliveryWorkerMutationResolver::class.java)

    /**
     * 配送员更新自己的资料
     */
    @PreAuthorize("hasRole('WORKER')")
    @Transactional
    fun updateMyDeliveryWorkerProfile(input: UpdateDeliveryWorkerProfileInput): DeliveryWorker {
        try {
            // 获取当前认证的配送员
            val currentWorkerId = getCurrentWorkerId()
            val existingWorker = deliveryWorkerRepository.findById(currentWorkerId)
                .orElseThrow { BadRequestException("配送员不存在: $currentWorkerId") }

            // 验证输入
            validateUpdateProfileInput(input)

            // 更新允许配送员修改的字段
            input.name?.let {
                if (it.isNotBlank()) {
                    existingWorker.name = it
                }
            }

            input.avatarUrl?.let { existingWorker.avatarUrl = it }
            input.coordinates?.let { existingWorker.coordinates = it }
            input.currentLocation?.let { existingWorker.currentLocation = it }

            // 配送员可以修改自己的在线状态
            input.isAvailable?.let { existingWorker.isAvailable = it }

            val updatedWorker = deliveryWorkerRepository.save(existingWorker)
            logger.info("Successfully updated delivery worker profile: ${updatedWorker.id} - ${updatedWorker.name}")
            return updatedWorker.let { DeliveryWorkerMapper.map(it) }
        } catch (e: Exception) {
            logger.error("Failed to update delivery worker profile", e)
            throw BadRequestException("更新配送员资料失败: ${e.message}")
        }
    }

    /**
     * 配送员更新自己的在线状态
     */
    @PreAuthorize("hasRole('WORKER')")
    @Transactional
    fun updateMyWorkerStatus(status: DeliverWorkerStatus): DeliveryWorker {
        try {
            val currentWorkerId = getCurrentWorkerId()
            val existingWorker = deliveryWorkerRepository.findById(currentWorkerId)
                .orElseThrow { BadRequestException("配送员不存在: $currentWorkerId") }

            existingWorker.onlineStatus = status
            val updatedWorker = deliveryWorkerRepository.save(existingWorker)

            logger.info("Successfully updated worker status: ${updatedWorker.id} - ${status}")
            return updatedWorker.let { DeliveryWorkerMapper.map(it) }
        } catch (e: Exception) {
            logger.error("Failed to update worker status", e)
            throw BadRequestException("更新工作状态失败: ${e.message}")
        }
    }

    /**
     * 配送员更新自己的位置信息
     */
    @PreAuthorize("hasRole('WORKER')")
    @Transactional
    fun updateMyLocation(coordinates: String, currentLocation: String): DeliveryWorker {
        try {
            val currentWorkerId = getCurrentWorkerId()
            val existingWorker = deliveryWorkerRepository.findById(currentWorkerId)
                .orElseThrow { BadRequestException("配送员不存在: $currentWorkerId") }

            existingWorker.coordinates = coordinates
            existingWorker.currentLocation = currentLocation

            val updatedWorker = deliveryWorkerRepository.save(existingWorker)
            logger.info("Successfully updated worker location: ${updatedWorker.id}")
            return DeliveryWorkerMapper.map(updatedWorker)
        } catch (e: Exception) {
            logger.error("Failed to update worker location", e)
            throw BadRequestException("更新位置信息失败: ${e.message}")
        }
    }

    /**
     * 验证更新资料输入
     */
    private fun validateUpdateProfileInput(input: UpdateDeliveryWorkerProfileInput) {
        // 配送员不能修改自己的微信OpenID、手机号、评分、收入
        // 这些字段由管理员维护

        input.name?.let {
            if (it.isBlank()) {
                throw BadRequestException("姓名不能为空")
            }
            if (it.length > 50) {
                throw BadRequestException("姓名长度不能超过50个字符")
            }
        }

        input.avatarUrl?.let {
            if (it.isNotBlank() && !isValidUrl(it)) {
                throw BadRequestException("头像URL格式不正确")
            }
        }
    }

    /**
     * 验证URL格式（简单验证）
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前认证配送员的ID
     * 在实际实现中应该从Spring Security Context获取
     */
    private fun getCurrentWorkerId(): Long {
        // TODO: 从Spring Security Context获取当前配送员ID
        // 暂时返回占位符，实际实现需要：
        // val authentication = SecurityContextHolder.getContext().authentication
        // return (authentication.principal as DeliveryWorkerDetails).id
        throw UnsupportedOperationException("需要从Spring Security Context获取配送员ID")
    }

    companion object {
        /**
         * 配送员资料更新输入类型
         * 注意：配送员只能修改部分字段，敏感字段由管理员维护
         */
        data class UpdateDeliveryWorkerProfileInput(
            val name: String?,           // 配送员可以修改姓名
            val avatarUrl: String?,       // 配送员可以修改头像
            val coordinates: String?,     // 配送员可以修改坐标
            val currentLocation: String?,  // 配送员可以修改当前位置
            val isAvailable: Boolean?    // 配送员可以修改可用状态
        )
    }
}
