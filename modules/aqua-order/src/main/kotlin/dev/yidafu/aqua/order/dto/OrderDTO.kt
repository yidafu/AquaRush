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

package dev.yidafu.aqua.order.dto

import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.User
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Data Transfer Object for Order information
 *
 * This DTO provides a clean separation between the domain model (OrderModel)
 * and the API layer. It includes nested objects for better API responses
 * and can be used for both GraphQL and REST endpoints.
 */
data class OrderDTO(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val user: User? = null,
    val productId: Long,
    val product: Product? = null,
    val quantity: Int,
    val amount: BigDecimal,
    val addressId: Long,
    val address: Address? = null,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod? = null,
    val paymentTransactionId: String? = null,
    val paymentTime: LocalDateTime? = null,
    val deliveryWorkerId: Long? = null,
    val deliveryWorker: DeliveryWorker? = null,
    val deliveryPhotos: List<String>? = null,
    val deliveryAddressId: Long,
    val completedAt: LocalDateTime? = null,
    val totalAmount: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * DTO for creating new orders
 */
data class CreateOrderDTO(
    val userId: Long,
    val productId: Long,
    val quantity: Int,
    val amount: BigDecimal,
    val addressId: Long,
    val deliveryAddressId: Long,
    val paymentMethod: PaymentMethod? = null
)

/**
 * DTO for updating order status
 */
data class UpdateOrderStatusDTO(
    val status: OrderStatus,
    val paymentMethod: PaymentMethod? = null,
    val paymentTransactionId: String? = null,
    val paymentTime: LocalDateTime? = null,
    val deliveryWorkerId: Long? = null,
    val deliveryPhotos: List<String>? = null,
    val completedAt: LocalDateTime? = null
)

/**
 * DTO for order queries with pagination
 */
data class OrderQueryDTO(
    val userId: Long? = null,
    val status: OrderStatus? = null,
    val paymentMethod: PaymentMethod? = null,
    val deliveryWorkerId: Long? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 20
)