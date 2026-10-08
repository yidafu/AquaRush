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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryTrajectoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * 配送轨迹上报服务单元测试(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03):
 * - 仅 DELIVERING 订单可上报,非配送中上报丢弃;
 * - 只能上报本人配送中订单的轨迹(防越权);
 * - 经纬度/精度合法性校验;
 * - 订单不存在视为不可上报。
 */
class DeliveryTrajectoryMutationServiceImplTest :
  FunSpec({
    var trajectoryRepository: DeliveryTrajectoryRepository = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var service: DeliveryTrajectoryMutationServiceImpl = mockk()

    fun order(
      status: OrderModelStatus,
      workerId: Long? = 1L,
    ) = OrderModel(id = 1L, userId = 100L, status = status, deliveryWorkerId = workerId)

    beforeTest {
      trajectoryRepository = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      // JpaRepository.save 泛型擦除,relaxed mock 无法推断返回类型,改为返回入参
      every { trajectoryRepository.save(any()) } answers { firstArg() as DeliveryTrajectoryModel }
      service =
        DeliveryTrajectoryMutationServiceImpl(
          trajectoryRepository = trajectoryRepository,
          orderQueryService = orderQueryService,
        )
    }
    test("report on delivering order by assigned worker saves point") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING, workerId = 1L)

      val point = service.reportTrajectory(1L, 1L, 116.397428, 39.90923, 5.0, LocalDateTime.of(2026, 8, 16, 10, 0, 0))

      point.deliveryWorkerId shouldBe 1L
      point.orderId shouldBe 1L
      point.longitude shouldBe 116.397428
      point.latitude shouldBe 39.90923
      point.accuracy shouldBe 5.0
      verify { trajectoryRepository.save(any()) }
    }
    test("report on non delivering order is rejected") {
      listOf(
        OrderModelStatus.PENDING_DELIVERY,
        OrderModelStatus.PENDING_DISPATCH,
        OrderModelStatus.COMPLETED,
        OrderModelStatus.CANCELLED,
      ).forEach { status ->
        every { orderQueryService.getOrderById(1L) } returns order(status, workerId = 1L)
        withClue("status=$status 应被拦截") {
          shouldThrow<BadRequestException> {
            service.reportTrajectory(1L, 1L, 116.397428, 39.90923, null, null)
          }
        }
      }
    }
    test("report on order assigned to another worker is rejected") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING, workerId = 2L)

      withClue("非本人订单应被拦截") {
        shouldThrow<BadRequestException> {
          service.reportTrajectory(1L, 1L, 116.397428, 39.90923, null, null)
        }
      }
    }
    test("report on missing order is rejected") {
      every { orderQueryService.getOrderById(1L) } throws NotFoundException("订单不存在")

      shouldThrow<BadRequestException> {
        service.reportTrajectory(1L, 1L, 116.397428, 39.90923, null, null)
      }
    }
    test("report with invalid coordinates is rejected") {
      every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING, workerId = 1L)

      listOf(
        Triple(181.0, 39.9, null),
        Triple(-181.0, 39.9, null),
        Triple(116.4, 91.0, null),
        Triple(116.4, -91.0, null),
        Triple(116.4, 39.9, -1.0),
      ).forEach { (lng, lat, acc) ->
        withClue("($lng,$lat,acc=$acc) 应被拦截") {
          shouldThrow<BadRequestException> {
            service.reportTrajectory(1L, 1L, lng, lat, acc, null)
          }
        }
      }
    }
  })
