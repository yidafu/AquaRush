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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 订单模型 → DTO → GraphQL 转换链单元测试。
 */
class OrderConversionChainTest {
  private val chain = OrderConversionChain()
  private val createdAt = LocalDateTime.of(2025, 1, 1, 10, 0)

  private fun orderModel() =
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

  private fun gqlUser() =
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

  private fun gqlAddress() =
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

  private fun gqlDeliveryWorker() =
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

  @Test
  @DisplayName("toDTO 转换并携带嵌套对象")
  fun `toDTO maps model with nested objects`() {
    val dto = chain.toDTO(orderModel(), gqlUser(), gqlAddress(), gqlDeliveryWorker())

    assertEquals("ORD-001", dto.orderNo)
    assertEquals(1200L, dto.amount)
    assertEquals(200L, dto.deliveryFeeCents)
    assertEquals(1400L, dto.payAmountCents)
    assertEquals(OrderModelStatus.PENDING_DISPATCH, dto.status)
    assertNotNull(dto.user)
    assertNotNull(dto.address)
    assertNotNull(dto.deliveryWorker)
    assertEquals(100L, dto.user?.id)
  }

  @Test
  @DisplayName("toDTO 无嵌套对象时保持 null")
  fun `toDTO keeps nested objects null by default`() {
    val dto = chain.toDTO(orderModel())

    assertTrue(dto.user == null)
    assertTrue(dto.address == null)
    assertTrue(dto.deliveryWorker == null)
  }

  @Test
  @DisplayName("toGraphQL 转换为 GraphQL Order")
  fun `toGraphQL maps to graphql order`() {
    val graphql = chain.toGraphQL(orderModel(), gqlUser(), gqlAddress(), gqlDeliveryWorker())

    assertEquals(1200L, graphql.amountCents)
    assertEquals(1400L, graphql.payAmountCents)
    assertEquals("ORD-001", graphql.orderNo)
    assertNotNull(graphql.user)
    assertNotNull(graphql.address)
    assertNotNull(graphql.deliveryWorker)
    assertEquals("WECHAT_PAY", graphql.paymentMethod)
  }

  @Test
  @DisplayName("toGraphQLList 使用查找函数填充嵌套对象")
  fun `toGraphQLList uses lookups`() {
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

    assertEquals(2, result.size)
    assertEquals(100L, result[0].user?.id)
    assertEquals(7L, result[0].address?.id)
    assertNotNull(result[0].deliveryWorker)
    // deliveryWorkerId 为 null 时不调用 lookup
    assertTrue(result[1].deliveryWorker == null)
  }

  @Test
  @DisplayName("toModel 以 DTO 字段覆盖并保留订单基础信息")
  fun `toModel applies dto fields onto existing model`() {
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

    assertEquals(OrderModelStatus.CANCELLED, updated.status)
    assertEquals(PaymentMethod.CASH, updated.paymentMethod)
    assertEquals(1L, updated.id)
    assertEquals("ORD-001", updated.orderNo)
    assertNotNull(updated.deliveryPhotos)
    assertTrue(updated.deliveryPhotos!!.contains("photo-1.jpg"))
  }

  @Test
  @DisplayName("toModel 无照片时保留原有照片")
  fun `toModel keeps existing photos when dto has none`() {
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

    assertEquals("""["old.jpg"]""", updated.deliveryPhotos)
    assertEquals(OrderModelStatus.COMPLETED, updated.status)
  }
}
