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

package dev.yidafu.aqua.client.notification.resolver

import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

/**
 * 用户端消息中心 Resolver 测试(PRD 13-notification.md F-NTF-002):
 * 仅本人消息(userId 强制取登录主体,防越权 R-AUTH-02);已读标记幂等透传。
 */
class ClientNotificationResolverTest :
  FunSpec({
    var queryApiService: NotificationQueryApiService = mockk()
    var mutationApiService: NotificationMutationApiService = mockk()
    var queryResolver: ClientNotificationQueryResolver = mockk()
    var mutationResolver: ClientNotificationMutationResolver = mockk()
    var userPrincipal: UserPrincipal = mockk()

    fun notification(
      id: Long,
      userId: Long = 100L,
      messageType: NotificationMessageType = NotificationMessageType.ORDER,
      read: Boolean = false,
    ): NotificationModel =
      NotificationModel(
        id = id,
        receiverType = NotificationReceiverType.USER,
        userId = userId,
        messageType = messageType,
        scene = "ORDER_PAID",
        title = "支付成功",
        summary = "订单已支付",
        relatedNo = "1001",
        idempotencyKey = "1001:ORDER_PAID:1715000000000$id",
        readAt = if (read) LocalDateTime.now() else null,
      )

    beforeTest {
      queryApiService = mockk(relaxed = true)
      mutationApiService = mockk(relaxed = true)
      queryResolver = ClientNotificationQueryResolver(queryApiService)
      mutationResolver = ClientNotificationMutationResolver(mutationApiService)
      userPrincipal =
        UserPrincipal(
          id = 100L,
          _username = "testuser",
          userType = "USER",
          _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }

    test("myNotifications 强制注入本人 userId 与分页") {
      every { queryApiService.findMyNotifications(any(), any(), any(), any(), any(), any()) } returns
        PageImpl(emptyList<NotificationModel>())

      queryResolver.myNotifications(type = null, read = null, page = 0, size = 10, userPrincipal)

      verify {
        queryApiService.findMyNotifications(
          receiverType = NotificationReceiverType.USER,
          receiverId = 100L,
          messageType = null,
          read = null,
          page = 0,
          size = 10,
        )
      }
    }

    test("myNotifications 透传已读/未读筛选(未读 Tab read=false,已读 Tab read=true)") {
      every { queryApiService.findMyNotifications(any(), any(), any(), any(), any(), any()) } returns
        PageImpl(emptyList<NotificationModel>())

      queryResolver.myNotifications(type = null, read = false, page = 0, size = 10, userPrincipal)
      queryResolver.myNotifications(type = null, read = true, page = 0, size = 10, userPrincipal)

      verify {
        queryApiService.findMyNotifications(
          receiverType = NotificationReceiverType.USER,
          receiverId = 100L,
          messageType = null,
          read = false,
          page = 0,
          size = 10,
        )
        queryApiService.findMyNotifications(
          receiverType = NotificationReceiverType.USER,
          receiverId = 100L,
          messageType = null,
          read = true,
          page = 0,
          size = 10,
        )
      }
    }

    test("myNotifications 映射未读状态与分页信息") {
      val page =
        PageImpl(
          listOf(notification(1L, read = false), notification(2L, read = true)),
          PageRequest.of(0, 10),
          2,
        )
      every { queryApiService.findMyNotifications(any(), any(), any(), any(), any(), any()) } returns page

      val result = queryResolver.myNotifications(type = null, read = null, page = 0, size = 10, userPrincipal)

      result.content.size shouldBe 2
      result.content[0].read shouldBe false
      result.content[1].read shouldBe true
      result.totalElements shouldBe 2
      result.totalPages shouldBe 1
    }

    test("unreadNotificationCount 透传本人未读数") {
      every { queryApiService.countUnread(NotificationReceiverType.USER, 100L) } returns 5L
      queryResolver.unreadNotificationCount(userPrincipal) shouldBe 5L
    }

    test("markNotificationRead 强制注入本人接收人信息") {
      every { mutationApiService.markRead(any(), any(), any()) } returns true
      mutationResolver.markNotificationRead(id = 1L, userPrincipal) shouldBe true
      verify {
        mutationApiService.markRead(
          receiverType = NotificationReceiverType.USER,
          receiverId = 100L,
          notificationId = 1L,
        )
      }
    }

    test("recordSubscribeAuth 透传用户接受的模板") {
      every { mutationApiService.recordSubscribeAuth(any(), any()) } returns 2
      mutationResolver.recordSubscribeAuth(templateIds = listOf("t1", "t2"), userPrincipal) shouldBe true
      verify {
        mutationApiService.recordSubscribeAuth(userId = 100L, templateIds = listOf("t1", "t2"))
      }
    }
  })
