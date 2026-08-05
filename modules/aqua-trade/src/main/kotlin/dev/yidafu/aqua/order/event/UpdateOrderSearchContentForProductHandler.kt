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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.utils.SearchContentBuilder
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class UpdateOrderSearchContentForProductHandler(
  private val orderRepository: OrderRepository,
  private val addressQueryApiService: AddressQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(UpdateOrderSearchContentForProductHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.PRODUCT_UPDATED

  @Transactional
  override fun handle(event: DomainEvent) {
    try {
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val productId = (eventData["productId"] as Number).toLong()
      logger.info("Updating search content for orders with product: $productId")

      // 查找使用该商品的所有订单
      val orders = orderRepository.findByProductId(productId)

      if (orders.isEmpty()) {
        logger.info("No orders found for product: $productId")
        return
      }

      // 获取商品信息
      val product = productQueryApiService.findById(productId) ?: return

      // 更新每个订单的搜索内容
      orders.forEach { order ->
        val address = order.addressId?.let { addressQueryApiService.findById(it) }
        val searchContent =
          SearchContentBuilder.buildSearchContent(
            address = address,
            product = product,
          )
        order.searchContent = searchContent
        orderRepository.save(order)
      }

      logger.info("Successfully updated search content for ${orders.size} orders")
    } catch (e: Exception) {
      logger.error("Failed to update order search content for product: ${event.id}", e)
      throw e
    }
  }
}
