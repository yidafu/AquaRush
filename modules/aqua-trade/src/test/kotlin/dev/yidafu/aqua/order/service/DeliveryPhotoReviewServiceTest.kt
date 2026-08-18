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

package dev.yidafu.aqua.order.service

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewAction
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 配送照片核验服务单元测试(G23/F-ORD-021):
 * 通过/驳回状态与留痕、驳回必填原因、无照片拦截、幂等可纠错。
 */
class DeliveryPhotoReviewServiceTest :
  FunSpec({
    var orderQueryService: OrderQueryService = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var orderOperationService: OrderOperationService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DeliveryPhotoReviewService = mockk()

    fun order(photos: String? = """["http://img/1.jpg"]""") =
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 100L,
        status = OrderModelStatus.COMPLETED,
        deliveryPhotos = photos,
      )

    beforeTest {
      orderQueryService = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      orderOperationService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service = DeliveryPhotoReviewService(orderQueryService, orderMutationService, orderOperationService, bizLogger)
      every { orderMutationService.updateOrder(any(), any()) } answers { secondArg() as OrderModel }
    }

    test("approve sets APPROVED with reviewer and remark") {
      val o = order()
      every { orderQueryService.getOrderById(1L) } returns o

      val saved = service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, "照片清晰", 7L, "admin1")

      saved.deliveryPhotoReviewStatus shouldBe DeliveryPhotoReviewStatus.APPROVED
      saved.deliveryPhotoReviewedBy shouldBe 7L
      saved.deliveryPhotoReviewRemark shouldBe "照片清晰"
      saved.deliveryPhotoReviewedAt.shouldNotBeNull()
      verify {
        orderOperationService.recordOperation(1L, OrderOperationType.DELIVERY_PHOTO_APPROVED, any(), 7L, any(), any())
      }
    }

    test("reject sets REJECTED and requires remark") {
      val o = order()
      every { orderQueryService.getOrderById(1L) } returns o

      val saved = service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, "照片模糊无法确认", 7L, "admin1")

      saved.deliveryPhotoReviewStatus shouldBe DeliveryPhotoReviewStatus.REJECTED
      verify {
        orderOperationService.recordOperation(1L, OrderOperationType.DELIVERY_PHOTO_REJECTED, any(), 7L, any(), any())
      }
    }

    test("reject without remark throws") {
      val o = order()
      every { orderQueryService.getOrderById(1L) } returns o

      shouldThrow<BadRequestException> { service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, null, 7L, "admin1") }
    }

    test("order without photos cannot be reviewed") {
      val o = order(photos = null)
      every { orderQueryService.getOrderById(1L) } returns o

      shouldThrow<BadRequestException> { service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, null, 7L, "admin1") }
    }

    test("re-review overwrites previous status (可纠错)") {
      val o = order()
      every { orderQueryService.getOrderById(1L) } returns o

      service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.REJECT, "先驳回", 7L, "admin1")
      val reApproved = service.reviewDeliveryPhoto(1L, DeliveryPhotoReviewAction.APPROVE, "复核通过", 8L, "admin2")

      reApproved.deliveryPhotoReviewStatus shouldBe DeliveryPhotoReviewStatus.APPROVED
      reApproved.deliveryPhotoReviewedBy shouldBe 8L
    }
  })
