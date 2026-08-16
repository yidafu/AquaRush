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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.admin.delivery.dto.ReportTrajectoryInput
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryMutationService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class DeliveryTrajectoryMutationResolverTest :
  FunSpec({
    var deliveryTrajectoryMutationService: DeliveryTrajectoryMutationService = mockk()
    var adminService: AdminService = mockk()
    var resolver: DeliveryTrajectoryMutationResolver = mockk()

    fun createSampleWorker(): AdminModel =
      AdminModel(
        id = 2L,
        username = "delivery_2",
        passwordHash = "hashed_password",
        realName = "送水员甲",
        phone = "13900139000",
        role = AdminRoleModel.DELIVERY_WORKER,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun workerPrincipal(): UserPrincipal =
      UserPrincipal(
        id = 2L,
        _username = "delivery_2",
        userType = "WORKER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_WORKER")),
      )

    beforeTest {
      deliveryTrajectoryMutationService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      resolver = DeliveryTrajectoryMutationResolver(deliveryTrajectoryMutationService, adminService)
    }
    test("reportTrajectory should report for delivery worker") {
      // Given
      every { adminService.findByUsername("delivery_2") } returns createSampleWorker()
      val input = ReportTrajectoryInput(orderId = 1L, longitude = 113.94f, latitude = 22.54f, accuracy = 5f)

      // When
      val result = resolver.reportTrajectory(input, workerPrincipal())

      // Then
      result shouldBe true
      verify {
        deliveryTrajectoryMutationService.reportTrajectory(
          deliveryWorkerId = 2L,
          orderId = 1L,
          longitude = any(),
          latitude = any(),
          accuracy = any(),
          reportedAt = any(),
        )
      }
    }
    test("reportTrajectory should default reportedAt to now") {
      // Given
      every { adminService.findByUsername("delivery_2") } returns createSampleWorker()
      val input = ReportTrajectoryInput(orderId = 1L, longitude = 113.94f, latitude = 22.54f, accuracy = null)

      // When
      val result = resolver.reportTrajectory(input, workerPrincipal())

      // Then
      result shouldBe true
      verify { deliveryTrajectoryMutationService.reportTrajectory(2L, 1L, any(), any(), null, any()) }
    }
    test("reportTrajectory should throw when admin not found") {
      // Given
      every { adminService.findByUsername("unknown") } returns null
      val user = UserPrincipal(9L, "unknown", "WORKER", emptyList())
      val input = ReportTrajectoryInput(orderId = 1L, longitude = 1f, latitude = 1f)

      // When & Then
      shouldThrow<UserNotFoundException> { resolver.reportTrajectory(input, user) }
    }
    test("reportTrajectory should reject non delivery roles") {
      // Given
      every { adminService.findByUsername("manager") } returns createSampleWorker().copy(role = AdminRoleModel.SUPER_ADMIN)
      val user = UserPrincipal(9L, "manager", "ADMIN", emptyList())
      val input = ReportTrajectoryInput(orderId = 1L, longitude = 1f, latitude = 1f)

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.reportTrajectory(input, user) }
      exception.message!!.contains("不具备配送能力") shouldBe true
    }
  })
