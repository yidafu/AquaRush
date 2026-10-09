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
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * OrderModel.toGraphQL 单元测试。
 * 覆盖配送备注(deliveryRemark)与下单备注(remark)映射 —— 完成配送后「配送信息」卡回显依据。
 */
class OrderMapperTest :
  FunSpec({
    test("OrderModel.toGraphQL exposes delivery remark and order remark") {
      val model =
        OrderModel(
          id = 1L,
          orderNo = "ORD-001",
          userId = 100L,
          amountCents = 1200,
          deliveryFeeCents = 200,
          addressId = 7L,
          status = OrderModelStatus.COMPLETED,
          remark = "下单备注",
          deliveryRemark = "放门口",
          createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
          updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        )

      val graphql = model.toGraphQL()

      graphql.remark shouldBe "下单备注"
      graphql.deliveryRemark shouldBe "放门口"
    }

    test("OrderModel.toGraphQL keeps delivery remark null when unset") {
      val model =
        OrderModel(
          id = 2L,
          orderNo = "ORD-002",
          userId = 100L,
          amountCents = 1200,
          deliveryFeeCents = 200,
          addressId = 7L,
          status = OrderModelStatus.DELIVERING,
          createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
          updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        )

      val graphql = model.toGraphQL()

      (graphql.deliveryRemark == null) shouldBe true
    }
  })
