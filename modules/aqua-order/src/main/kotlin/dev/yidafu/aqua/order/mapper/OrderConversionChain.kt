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

package dev.yidafu.aqua.order.mapper

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.order.dto.OrderDTO
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Utility class that provides the complete conversion chain:
 * OrderModel -> OrderDTO -> GraphQL Order
 *
 * This class handles nested object mapping and ensures all relationships
 * are properly populated for GraphQL responses.
 */
@Component
class OrderConversionChain {

    /**
     * Convert OrderModel to GraphQL Order with full nested object support
     */
    fun toGraphQL(
        orderModel: OrderModel,
        user: User? = null,
        product: Product? = null,
        address: Address? = null,
        deliveryWorker: DeliveryWorker? = null
    ): Order {
        // First convert to DTO
        val orderDTO = OrderModelToDTOMapper.map(orderModel).copy(
            user = user,
            product = product,
            address = address,
            deliveryWorker = deliveryWorker
        )

        // Then convert to GraphQL
        return OrderDTOToGraphQLMapper.map(orderDTO)
    }

    /**
     * Convert OrderModel to OrderDTO with optional nested objects
     */
    fun toDTO(
        orderModel: OrderModel,
        user: User? = null,
        product: Product? = null,
        address: Address? = null,
        deliveryWorker: DeliveryWorker? = null
    ): OrderDTO {
        return OrderModelToDTOMapper.map(orderModel).copy(
            user = user,
            product = product,
            address = address,
            deliveryWorker = deliveryWorker
        )
    }

    /**
     * Convert GraphQL Order to OrderDTO
     */
    fun fromGraphQL(graphqlOrder: Order): OrderDTO {
        return GraphQLToOrderDTOMapper.map(graphqlOrder)
    }

    /**
     * Convert OrderDTO back to OrderModel (for updates)
     */
    fun toModel(orderDTO: OrderDTO, existingOrderModel: OrderModel): OrderModel {
        // Since OrderModel is a JPA entity, we can't use copy()
        // Instead, we create a new OrderModel with updated fields
        return OrderModel(
            id = existingOrderModel.id,
            orderNumber = existingOrderModel.orderNumber,
            userId = existingOrderModel.userId,
            productId = existingOrderModel.productId,
            quantity = existingOrderModel.quantity,
            amount = existingOrderModel.amount,
            addressId = existingOrderModel.addressId,
            deliveryAddressId = existingOrderModel.deliveryAddressId,
            status = orderDTO.status,
            paymentMethod = orderDTO.paymentMethod ?: existingOrderModel.paymentMethod,
            totalAmount = orderDTO.totalAmount,
            deliveryPhotos = orderDTO.deliveryPhotos?.let { photos ->
                jacksonObjectMapper().writeValueAsString(photos)
            } ?: existingOrderModel.deliveryPhotos,
            paymentTransactionId = orderDTO.paymentTransactionId ?: existingOrderModel.paymentTransactionId,
            paymentTime = orderDTO.paymentTime ?: existingOrderModel.paymentTime,
            deliveryWorkerId = orderDTO.deliveryWorkerId ?: existingOrderModel.deliveryWorkerId,
            completedAt = orderDTO.completedAt ?: existingOrderModel.completedAt,
            createdAt = existingOrderModel.createdAt,
            updatedAt = java.time.LocalDateTime.now() // Update timestamp
        )
    }

    /**
     * Convert a list of OrderModels to GraphQL Orders
     */
    fun toGraphQLList(
        orderModels: List<OrderModel>,
        // Provide lookup functions for nested objects
        userLookup: (Long) -> User? = { null },
        productLookup: (Long) -> Product? = { null },
        addressLookup: (Long) -> Address? = { null },
        deliveryWorkerLookup: (Long?) -> DeliveryWorker? = { null }
    ): List<Order> {
        return orderModels.map { orderModel ->
            toGraphQL(
                orderModel = orderModel,
                user = userLookup(orderModel.userId),
                product = productLookup(orderModel.productId),
                address = addressLookup(orderModel.addressId),
                deliveryWorker = orderModel.deliveryWorkerId?.let(deliveryWorkerLookup)
            )
        }
    }
}
