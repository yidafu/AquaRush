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

package dev.yidafu.aqua.admin.notification.resolvers

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

/**
 * 送水工端消息中心 Resolver 测试(PRD 13-notification.md F-NTF-004):
 * workerId = 登录账号 id(送水员档案并入 admins 单表);仅本人消息隔离。
 */
class WorkerNotificationResolverTest :
  FunSpec({
    var queryApiService: NotificationQueryApiService = mockk()
    var mutationApiService: NotificationMutationApiService = mockk()
    var adminService: AdminService = mockk()
    var resolver: WorkerNotificationResolver = mockk()
    var userDetails: UserDetails = mockk()

    fun notification(id: Long): NotificationModel =
      NotificationModel(
        id = id,
        receiverType = NotificationReceiverType.WORKER,
        workerId = 9L,
        messageType = NotificationMessageType.ORDER,
        scene = "DELIVERY_ASSIGNED",
        title = "新派单",
        relatedNo = "1001",
        idempotencyKey = "1001:DELIVERY_ASSIGNED:1715000000000$id",
      )

    beforeTest {
      queryApiService = mockk(relaxed = true)
      mutationApiService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      resolver = WorkerNotificationResolver(queryApiService, mutationApiService, adminService)
      userDetails =
        User.withUsername("delivery_9").password("x").authorities("PERMISSION_DELIVERY_TASK").build()
      every { adminService.findByUsername("delivery_9") } returns AdminModel(id = 9L, username = "delivery_9", passwordHash = "x")
    }

    test("myWorkerNotifications 以登录账号 id 作为 workerId 查询本人消息") {
      every { queryApiService.findMyNotifications(any(), any(), any(), any(), any()) } returns
        PageImpl(emptyList<NotificationModel>())

      resolver.myWorkerNotifications(type = null, page = 0, size = 10, userDetails)

      verify {
        queryApiService.findMyNotifications(
          receiverType = NotificationReceiverType.WORKER,
          receiverId = 9L,
          messageType = null,
          page = 0,
          size = 10,
        )
      }
    }

    test("workerUnreadNotificationCount 返回本人未读数") {
      every { queryApiService.countUnread(NotificationReceiverType.WORKER, 9L) } returns 3L
      resolver.workerUnreadNotificationCount(userDetails) shouldBe 3L
    }

    test("markWorkerNotificationRead 强制本人接收人信息") {
      every { mutationApiService.markRead(any(), any(), any()) } returns true
      resolver.markWorkerNotificationRead(id = 5L, userDetails) shouldBe true
      verify {
        mutationApiService.markRead(
          receiverType = NotificationReceiverType.WORKER,
          receiverId = 9L,
          notificationId = 5L,
        )
      }
    }
  })
