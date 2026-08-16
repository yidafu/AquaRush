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

package dev.yidafu.aqua.order.mapper

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.order.dto.OrderDTO
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * 订单模型 → DTO → GraphQL 转换链单元测试。
 */
class OrderConversionChainTest :
  FunSpec({
    val chain = OrderConversionChain()
    val createdAt = LocalDateTime.of(2025, 1, 1, 10, 0)

    fun orderModel() =
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 100L,
        amountCents = 1200,
        deliveryFeeCents = 200,
        addressId = 7L,
        status = OrderModelStatus.PENDING_DISPATCH,
        paymentMethod = PaymentMethod.WECHAT_PAY,
        paymentTransactionId = "WX-TXN-1",
        deliveryWorkerId = 5L,
        isSelfCollect = false,
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    fun gqlUser() =
      User(
        wechatOpenId = "openid-1",
        id = 100L,
        createdAt = createdAt,
        customerGroupId = null,
        nickname = "小明",
        avatarUrl = null,
        balanceCents = 0L,
        email = "",
        phone = "13800138000",
        role = UserRole.USER,
        status = UserStatus.ACTIVE,
        totalSpentCents = 0L,
        updatedAt = createdAt,
        customerGroupName = null,
      )

    fun gqlAddress() =
      Address(
        city = "杭州市",
        cityCode = "330100",
        createdAt = createdAt,
        detailAddress = "文一西路100号",
        district = "西湖区",
        districtCode = "330106",
        floor = 4,
        id = 7L,
        isDefault = true,
        latitude = null,
        longitude = null,
        phone = "13800138000",
        province = "浙江省",
        provinceCode = "330000",
        receiverName = "张三",
        updatedAt = createdAt,
        userId = 100L,
      )

    fun gqlDeliveryWorker() =
      DeliveryWorker(
        accountStatus = dev.yidafu.aqua.common.graphql.generated.AdminAccountStatus.ACTIVE,
        avatarUrl = null,
        averageRating = java.math.BigDecimal.valueOf(4.5),
        completedOrders = 10,
        coordinates = null,
        createdAt = createdAt,
        currentLocation = null,
        currentTaskCount = 0,
        earningCents = 10000L,
        id = 5L,
        isAvailable = true,
        name = "送水员",
        onlineStatus = dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus.ONLINE,
        phone = "13900000000",
        rating = java.math.BigDecimal.valueOf(4.5),
        role = "DELIVERY_WORKER",
        totalOrders = 20,
        updatedAt = createdAt,
        userId = 5L,
        wechatOpenId = "",
      )

    test("toDTO maps model with nested objects") {
      val dto = chain.toDTO(orderModel(), gqlUser(), gqlAddress(), gqlDeliveryWorker())

      dto.orderNo shouldBe "ORD-001"
      dto.amount shouldBe 1200L
      dto.deliveryFeeCents shouldBe 200L
      dto.payAmountCents shouldBe 1400L
      dto.status shouldBe OrderModelStatus.PENDING_DISPATCH
      dto.user.shouldNotBeNull()
      dto.address.shouldNotBeNull()
      dto.deliveryWorker.shouldNotBeNull()
      dto.user?.id shouldBe 100L
    }
    test("toDTO keeps nested objects null by default") {
      val dto = chain.toDTO(orderModel())

      (dto.user == null) shouldBe true
      (dto.address == null) shouldBe true
      (dto.deliveryWorker == null) shouldBe true
    }
    test("toGraphQL maps to graphql order") {
      val graphql = chain.toGraphQL(orderModel(), gqlUser(), gqlAddress(), gqlDeliveryWorker())

      graphql.amountCents shouldBe 1200L
      graphql.payAmountCents shouldBe 1400L
      graphql.orderNo shouldBe "ORD-001"
      graphql.user.shouldNotBeNull()
      graphql.address.shouldNotBeNull()
      graphql.deliveryWorker.shouldNotBeNull()
      graphql.paymentMethod shouldBe "WECHAT_PAY"
    }
    test("toGraphQLList uses lookups") {
      val order1 = orderModel()
      val order2 =
        orderModel().also {
          it.id = 2L
          it.deliveryWorkerId = null
        }

      val result =
        chain.toGraphQLList(
          listOf(order1, order2),
          userLookup = { gqlUser() },
          addressLookup = { gqlAddress() },
          deliveryWorkerLookup = { gqlDeliveryWorker() },
        )

      result.size shouldBe 2
      result[0].user?.id shouldBe 100L
      result[0].address?.id shouldBe 7L
      result[0].deliveryWorker.shouldNotBeNull()
      // deliveryWorkerId 为 null 时不调用 lookup
      (result[1].deliveryWorker == null) shouldBe true
    }
    test("toModel applies dto fields onto existing model") {
      val dto =
        OrderDTO(
          id = 1L,
          orderNo = "ORD-001",
          userId = 100L,
          amount = 1500,
          deliveryFeeCents = 0,
          addressId = 7L,
          status = OrderModelStatus.CANCELLED,
          paymentMethod = PaymentMethod.CASH,
          deliveryPhotos = listOf("photo-1.jpg"),
          totalAmount = 1500,
          createdAt = createdAt,
          updatedAt = createdAt,
        )

      val updated = chain.toModel(dto, orderModel())

      updated.status shouldBe OrderModelStatus.CANCELLED
      updated.paymentMethod shouldBe PaymentMethod.CASH
      updated.id shouldBe 1L
      updated.orderNo shouldBe "ORD-001"
      updated.deliveryPhotos.shouldNotBeNull()
      updated.deliveryPhotos!!.contains("photo-1.jpg") shouldBe true
    }
    test("toModel keeps existing photos when dto has none") {
      val existing = orderModel().also { it.deliveryPhotos = """["old.jpg"]""" }
      val dto =
        OrderDTO(
          id = 1L,
          orderNo = "ORD-001",
          userId = 100L,
          amount = 1200,
          addressId = 7L,
          status = OrderModelStatus.COMPLETED,
          totalAmount = 1200,
          createdAt = createdAt,
          updatedAt = createdAt,
        )

      val updated = chain.toModel(dto, existing)

      updated.deliveryPhotos shouldBe """["old.jpg"]"""
      updated.status shouldBe OrderModelStatus.COMPLETED
    }
  })
