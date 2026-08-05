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
class UpdateOrderSearchContentHandler(
  private val orderRepository: OrderRepository,
  private val addressQueryApiService: AddressQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(UpdateOrderSearchContentHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.ADDRESS_UPDATED

  @Transactional
  override fun handle(event: DomainEvent) {
    try {
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val addressId = (eventData["addressId"] as Number).toLong()
      logger.info("Updating search content for orders with address: $addressId")

      // 查找使用该地址的所有订单
      val orders = orderRepository.findByAddressId(addressId)

      if (orders.isEmpty()) {
        logger.info("No orders found for address: $addressId")
        return
      }

      // 获取地址信息
      val address = addressQueryApiService.findById(addressId) ?: return

      // 更新每个订单的搜索内容
      orders.forEach { order ->
        val product = order.productId?.let { productQueryApiService.findById(it) }
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
      logger.error("Failed to update order search content: ${event.id}", e)
      throw e
    }
  }
}
