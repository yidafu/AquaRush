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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.common.graphql.generated.AddressVo
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.graphql.generated.UserVo
import java.time.LocalDateTime

/**
 * 测试夹具:构造真实 OrderVo(非 mock),用于断言号码保护脱敏(F-ORD-032)输出形态。
 * 生成类无默认参数,集中补全避免各测试重复。
 */
internal fun testOrderVo(
  status: OrderStatus,
  addressPhone: String? = "13812341234",
  userPhone: String? = "13812341234",
): OrderVo {
  val now = LocalDateTime.now()
  return OrderVo(
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
    // 其余快照/留痕字段与号码保护断言无关,统一置空
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
}
