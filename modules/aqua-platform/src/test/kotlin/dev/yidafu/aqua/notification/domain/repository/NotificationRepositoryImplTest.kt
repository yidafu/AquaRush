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

package dev.yidafu.aqua.notification.domain.repository

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.exposed.PlatformExposedH2TestSupport
import dev.yidafu.aqua.common.exposed.PlatformExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class NotificationRepositoryImplTest :
  FunSpec({
    val repo = NotificationRepositoryImpl(PlatformExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增/更新与 findById/existsByIdempotencyKey") {
      val saved =
        repo.save(
          NotificationModel(
            receiverType = NotificationReceiverType.USER,
            userId = 1L,
            messageType = NotificationMessageType.SYSTEM,
            scene = "order",
            title = "标题",
            idempotencyKey = "k1",
          ),
        )
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .title shouldBe "标题"
      repo.existsByIdempotencyKey("k1") shouldBe true
      repo.existsByIdempotencyKey("nope") shouldBe false

      repo.save(saved.apply { channelStatus = dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus.FAILED })
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .channelStatus.name shouldBe "FAILED"
    }

    test("findMyNotifications:USER/WORKER 接收人与已读过滤") {
      PlatformExposedH2TestSupport.notification(idempotencyKey = "u1", userId = 1L)
      PlatformExposedH2TestSupport.notification(idempotencyKey = "u2", userId = 1L, readAt = LocalDateTime.now())
      PlatformExposedH2TestSupport.notification(
        idempotencyKey = "w1",
        receiverType = "WORKER",
        userId = null,
        workerId = 9L,
      )

      repo
        .findMyNotifications(NotificationReceiverType.USER, 1L, null, null, PageRequest.of(0, 10))
        .totalElements shouldBe 2L
      repo
        .findMyNotifications(NotificationReceiverType.USER, 1L, null, false, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .findMyNotifications(NotificationReceiverType.USER, 1L, null, true, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .findMyNotifications(
          NotificationReceiverType.USER,
          1L,
          NotificationMessageType.SYSTEM,
          null,
          PageRequest.of(0, 10),
        ).totalElements shouldBe 2L
      repo
        .findMyNotifications(NotificationReceiverType.WORKER, 9L, null, null, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
    }

    test("countUnread 与 markAllRead(幂等)") {
      PlatformExposedH2TestSupport.notification(idempotencyKey = "a", userId = 1L)
      PlatformExposedH2TestSupport.notification(idempotencyKey = "b", userId = 1L)

      repo.countUnread(NotificationReceiverType.USER, 1L) shouldBe 2L
      repo.markAllRead(NotificationReceiverType.USER, 1L) shouldBe 2
      repo.countUnread(NotificationReceiverType.USER, 1L) shouldBe 0L
      repo.markAllRead(NotificationReceiverType.USER, 1L) shouldBe 0
    }

    test("findByFilters:时间窗/关键词/接收类型") {
      PlatformExposedH2TestSupport.notification(idempotencyKey = "f1", title = "订单已送达")
      PlatformExposedH2TestSupport.notification(idempotencyKey = "f2", title = "余额提醒")
      PlatformExposedH2TestSupport.notification(
        idempotencyKey = "f3",
        receiverType = "WORKER",
        userId = null,
        workerId = 3L,
      )

      val req = NotificationListRequest(keyword = "送达")
      repo.findByFilters(req, PageRequest.of(0, 10)).totalElements shouldBe 1L

      val byType =
        NotificationListRequest(receiverType = NotificationReceiverType.WORKER)
      repo.findByFilters(byType, PageRequest.of(0, 10)).totalElements shouldBe 1L

      val byTime =
        NotificationListRequest(startAt = LocalDateTime.now().plusMinutes(1))
      repo.findByFilters(byTime, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("deleteExpired 软删保留策略") {
      val old = LocalDateTime.now().minusDays(1)
      PlatformExposedH2TestSupport.notification(idempotencyKey = "old", createdAt = old)
      PlatformExposedH2TestSupport.notification(idempotencyKey = "new", createdAt = LocalDateTime.now().plusMinutes(5))

      repo.deleteExpired(LocalDateTime.now()) shouldBe 1
      // 软删后旧行不再计入未读,新行保留
      repo.countUnread(NotificationReceiverType.USER, 1L) shouldBe 1L
    }
  })
