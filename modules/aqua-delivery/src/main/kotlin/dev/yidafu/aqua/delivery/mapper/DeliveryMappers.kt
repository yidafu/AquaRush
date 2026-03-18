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

package dev.yidafu.aqua.delivery.mapper

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.DeliveryArea
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import java.time.LocalDateTime

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type
 */
object DeliveryWorkerMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel): DeliveryWorker =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::userId fromValue (from.userId ?: 0L)
      // Fields with same name and type - auto-mapped by Mappie
      to::earning fromProperty from::earningCents
      to::onlineStatus fromExpression {
        DeliveryWorkerStatus.valueOf(from.onlineStatus.name)
      }
      to::currentTaskCount fromValue 0
    }
}

/**
 * Mapper for converting DeliveryWorkerModel domain entity to GraphQL DeliveryWorker type (with current user)
 */
object DeliveryWorkerWithCurrentUserMapper : ObjectMappie<DeliveryWorkerModel, DeliveryWorker>() {
  override fun map(from: DeliveryWorkerModel): DeliveryWorker =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::userId fromValue (from.userId ?: 0L)
      // Fields with same name and type - auto-mapped by Mappie
      to::earning fromProperty from::earningCents
      to::onlineStatus fromExpression {
        DeliveryWorkerStatus.valueOf(from.onlineStatus.name)
      }
      to::currentTaskCount fromValue 0
    }
}

/**
 * Mapper for converting DeliveryAreaModel domain entity to GraphQL DeliveryArea type
 */
object DeliveryAreaMapper : ObjectMappie<DeliveryAreaModel, DeliveryArea>() {
  override fun map(from: DeliveryAreaModel): DeliveryArea =
    mapping {
      // All fields have same name and type - auto-mapped by Mappie
    }
}

/**
 * Enum mapper for WorkerStatus domain enum to GraphQL WorkerStatus enum
 */
object DeliveryWorkerStatusMapper : EnumMappie<DeliverWorkerModelStatus, DeliveryWorkerStatus>()

/**
 * Mapper for converting OrderModel to GraphQL Order type
 * This mapper creates basic order data without nested relationships
 * For full order data with user, product, and address, use the full mapper in order module
 */
object OrderMapper : ObjectMappie<OrderModel, Order>() {
  override fun map(from: OrderModel): Order =
    mapping {
      to::id fromValue (from.id ?: -1L)
      to::orderNo fromProperty from::orderNo
      to::quantity fromProperty from::quantity
      to::amount fromProperty from::amountCents
      to::isSelfCollect fromProperty from::isSelfCollect
      to::status fromExpression { OrderStatus.valueOf(from.status.name) }
      to::createdAt fromProperty from::createdAt
      to::updatedAt fromProperty from::updatedAt
      // Nullable fields
      to::completedAt fromProperty from::completedAt
      to::deliveryStartedAt fromProperty from::deliveryStartedAt
      to::deliveryConfirmedAt fromProperty from::deliveryConfirmedAt
      to::deliveryPhotos fromExpression {
        from.deliveryPhotos?.split(",")?.let { emptyList<String>().plus(it) } ?: emptyList()
      }
      to::paymentMethod fromExpression { from.paymentMethod?.name }
      to::paymentTime fromProperty from::paymentTime
      to::paymentTransactionId fromProperty from::paymentTransactionId
      to::paymentType fromProperty from::paymentType
      to::remark fromProperty from::remark
      // Nested objects - create placeholder values for required fields
      to::user fromExpression { createPlaceholderUser(from.userId ?: 0L) }
      to::product fromExpression { createPlaceholderProduct(from.productId) }
      to::address fromExpression { createPlaceholderAddress(from.addressId) }
      to::deliveryWorker fromExpression { null }
    }

  private fun createPlaceholderUser(userId: Long) =
    dev.yidafu.aqua.common.graphql.generated.User(
      avatarUrl = null,
      balanceCents = 0L,
      createdAt = java.time.LocalDateTime.now(),
      email = "",
      id = userId,
      nickname = null,
      phone = null,
      role = dev.yidafu.aqua.common.graphql.generated.UserRole.USER,
      status = dev.yidafu.aqua.common.graphql.generated.UserStatus.ACTIVE,
      totalSpentCents = 0L,
      updatedAt = java.time.LocalDateTime.now(),
      wechatOpenId = "",
    )

  private fun createPlaceholderProduct(productId: Long) =
    Product(
      certificateImages = null,
      coverImageUrl = "",
      createdAt = LocalDateTime.now(),
      deliverySettings = null,
      depositPrice = null,
      detailContent = null,
      id = productId,
      imageGallery = null,
      mineralContent = null,
      name = "",
      originalPrice = null,
      price = 0L,
      salesVolume = 0,
      sortOrder = 0,
      specification = "",
      status = ProductStatus.OFFLINE,
      stock = 0,
      subtitle = null,
      tags = null,
      updatedAt = LocalDateTime.now(),
      waterSource = null,
    )

  private fun createPlaceholderAddress(addressId: Long) =
    Address(
      city = "",
      cityCode = null,
      createdAt = LocalDateTime.now(),
      detailAddress = "",
      district = "",
      districtCode = null,
      id = addressId,
      isDefault = false,
      latitude = null,
      longitude = null,
      phone = "",
      province = "",
      provinceCode = null,
      receiverName = "",
      updatedAt = LocalDateTime.now(),
      userId = 0L,
    )
}
