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

import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.AddressVo
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.graphql.generated.UserVo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * 号码保护(F-ORD-032/R-ORD-17)脱敏纯函数单元测试:
 * 覆盖固定形态「前 3 + **** + 后 4」、短号/空号边界、7 状态可见性窗口与 OrderVo 组装脱敏。
 */
class PhoneMaskingTest :
  FunSpec({
    val now = LocalDateTime.now()

    fun orderVo(
      status: OrderStatus,
      addressPhone: String? = "13812341234",
      userPhone: String? = "13812341234",
    ): OrderVo =
      OrderVo(
        id = 1L,
        orderNo = "ORD-20250101-001",
        amountCents = 1000,
        deliveryFeeCents = 0,
        payAmountCents = 1000,
        depositCents = 0,
        status = status,
        isSelfCollect = false,
        items = emptyList(),
        createdAt = now,
        updatedAt = now,
        // 其余快照/留痕字段与本测试无关,统一置空
        assignedAt = null,
        completedAt = null,
        couponDiscountCents = null,
        couponName = null,
        deliveryPhotoReviewRemark = null,
        deliveryPhotoReviewStatus = null,
        deliveryPhotoReviewedAt = null,
        deliveryPhotoReviewedBy = null,
        deliveryPhotos = null,
        deliveryRemark = null,
        deliveryStartedAt = null,
        deliveryTimeSlot = null,
        deliveryWorker = null,
        firstOrderActivityName = null,
        firstOrderDiscountCents = null,
        giftItems = null,
        paymentMethod = null,
        paymentTime = null,
        paymentTransactionId = null,
        paymentType = null,
        promotionActivityName = null,
        promotionDiscountCents = null,
        remark = null,
        ticketDiscountCents = null,
        ticketRedeemCount = null,
        address =
          addressPhone?.let {
            AddressVo(
              id = 10L,
              userId = 1L,
              receiverName = "张三",
              phone = it,
              province = "浙江省",
              provinceCode = "330000",
              city = "杭州市",
              cityCode = "330100",
              district = "西湖区",
              districtCode = "330106",
              detailAddress = "文一路 1 号",
              floor = 3,
              longitude = null,
              latitude = null,
              isDefault = true,
              createdAt = now,
              updatedAt = now,
            )
          },
        user =
          UserVo(
            id = 1L,
            nickname = "客户甲",
            phone = userPhone,
            email = "",
            wechatOpenId = "openid",
            avatarUrl = null,
            customerGroupId = null,
            customerGroupName = null,
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            totalSpentCents = 0,
            createdAt = now,
            updatedAt = now,
          ),
      )

    context("maskCustomerPhone 固定形态") {
      test("11 位手机号脱敏为前 3 + **** + 后 4") {
        maskCustomerPhone("13812341234") shouldBe "138****1234"
      }

      test("8 位边界号码可无重叠截取") {
        maskCustomerPhone("12345678") shouldBe "123****5678"
      }

      test("不足 8 位整段置 **** 防前 3/后 4 重叠泄漏") {
        maskCustomerPhone("1234567") shouldBe "****"
        maskCustomerPhone("12345") shouldBe "****"
        maskCustomerPhone("123") shouldBe "****"
      }

      test("空白号码原样返回(无可泄漏内容)") {
        maskCustomerPhone("") shouldBe ""
        maskCustomerPhone("   ") shouldBe "   "
      }

      test("非 11 位标准格式仍按固定形态截取") {
        maskCustomerPhone("0571-87654321") shouldBe "057****4321"
      }
    }

    context("R-ORD-17 状态可见性窗口") {
      test("仅待配送/配送中可见完整号码") {
        OrderModelStatus.entries.filter { it.isFullPhoneVisibleForWorker() } shouldContainExactly
          listOf(OrderModelStatus.PENDING_DELIVERY, OrderModelStatus.DELIVERING)
      }

      test("7 状态逐一判定:非配送阶段一律脱敏(AC-ORD-032-02/03)") {
        val maskedStatuses =
          mapOf(
            OrderModelStatus.PENDING_PAYMENT to OrderStatus.PENDING_PAYMENT,
            OrderModelStatus.PENDING_DISPATCH to OrderStatus.PENDING_DISPATCH,
            OrderModelStatus.COMPLETED to OrderStatus.COMPLETED,
            OrderModelStatus.CANCELLED to OrderStatus.CANCELLED,
            OrderModelStatus.REFUNDED to OrderStatus.REFUNDED,
          )
        maskedStatuses.forEach { (domain, _) ->
          maskCustomerPhoneForStatus("13812341234", domain) shouldBe "138****1234"
        }
        maskCustomerPhoneForStatus("13812341234", OrderModelStatus.PENDING_DELIVERY) shouldBe "13812341234"
        maskCustomerPhoneForStatus("13812341234", OrderModelStatus.DELIVERING) shouldBe "13812341234"
      }

      test("可空账号电话由 OrderVo 组装层处理(null 透传)") {
        val vo = orderVo(OrderStatus.COMPLETED, userPhone = null).maskCustomerContactsForWorker(OrderModelStatus.COMPLETED)
        vo.user?.phone shouldBe null
        vo.address?.phone shouldBe "138****1234"
      }
    }

    context("OrderVo.maskCustomerContactsForWorker 组装脱敏") {
      test("非配送阶段同时脱敏收货人电话与下单账号电话(防经账号电话绕过)") {
        val vo = orderVo(OrderStatus.PENDING_DISPATCH).maskCustomerContactsForWorker(OrderModelStatus.PENDING_DISPATCH)

        vo.address?.phone shouldBe "138****1234"
        vo.user?.phone shouldBe "138****1234"
        // 其余字段不受影响
        vo.orderNo shouldBe "ORD-20250101-001"
        vo.amountCents shouldBe 1000
      }

      test("配送阶段保持完整号码(AC-ORD-032-01)") {
        val vo = orderVo(OrderStatus.DELIVERING).maskCustomerContactsForWorker(OrderModelStatus.DELIVERING)

        vo.address?.phone shouldBe "13812341234"
        vo.user?.phone shouldBe "13812341234"
      }

      test("地址或账号缺失时安全脱敏") {
        val noAddress =
          orderVo(OrderStatus.COMPLETED, addressPhone = null).maskCustomerContactsForWorker(OrderModelStatus.COMPLETED)
        noAddress.address shouldBe null
        noAddress.user?.phone shouldBe "138****1234"

        val noUser =
          orderVo(OrderStatus.COMPLETED, userPhone = null).maskCustomerContactsForWorker(OrderModelStatus.COMPLETED)
        noUser.address?.phone shouldBe "138****1234"
        noUser.user?.phone shouldBe null
      }
    }
  })
