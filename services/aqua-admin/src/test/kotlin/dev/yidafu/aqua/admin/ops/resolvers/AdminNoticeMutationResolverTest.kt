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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.NoticeMutationService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.common.graphql.generated.CreateNoticeInput
import dev.yidafu.aqua.common.graphql.generated.UpdateNoticeInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.NoticeType as GraphQLNoticeType

class AdminNoticeMutationResolverTest :
  FunSpec({
    var noticeMutationService: NoticeMutationService = mockk()
    var resolver: AdminNoticeMutationResolver = mockk()

    fun notice(): NoticeModel =
      NoticeModel(
        id = 1L,
        title = "系统维护通知",
        content = "本周六凌晨维护",
        type = NoticeType.PLATFORM_NOTICE,
        status = NoticeStatus.PUBLISHED,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      noticeMutationService = mockk(relaxed = true)
      resolver = AdminNoticeMutationResolver(noticeMutationService)
    }
    test("createNotice should delegate") {
      // Given
      every { noticeMutationService.createNotice(1L, "admin", any()) } returns notice()
      val input = CreateNoticeInput(content = "本周六凌晨维护", title = "系统维护通知", type = GraphQLNoticeType.PLATFORM_NOTICE)

      // When
      val result = resolver.createNotice(input, principal())

      // Then
      result.title shouldBe "系统维护通知"
      verify { noticeMutationService.createNotice(1L, "admin", any()) }
    }
    test("publishNotice should delegate") {
      // Given
      every { noticeMutationService.publishNotice(1L, "admin", 1L) } returns notice()

      // When
      val result = resolver.publishNotice(1L, principal())

      // Then
      result.title shouldBe "系统维护通知"
      verify { noticeMutationService.publishNotice(1L, "admin", 1L) }
    }
    test("stopNotice should delegate") {
      // Given
      every { noticeMutationService.stopNotice(1L, "admin", 1L) } returns notice()

      // When
      val result = resolver.stopNotice(1L, principal())

      // Then
      NoticeStatus.valueOf(result.status.name) shouldBe NoticeStatus.PUBLISHED
      verify { noticeMutationService.stopNotice(1L, "admin", 1L) }
    }
    test("setNoticePinned should delegate") {
      // Given
      every { noticeMutationService.setNoticePinned(1L, "admin", 1L, true) } returns
        NoticeModel(
          id = 1L,
          title = "系统维护通知",
          content = "本周六凌晨维护",
          type = NoticeType.PLATFORM_NOTICE,
          status = NoticeStatus.PUBLISHED,
          pinned = true,
          createdAt = LocalDateTime.now(),
          updatedAt = LocalDateTime.now(),
        )

      // When
      val result = resolver.setNoticePinned(1L, true, principal())

      // Then
      result.pinned shouldBe true
      verify { noticeMutationService.setNoticePinned(1L, "admin", 1L, true) }
    }
    test("updateNotice should delegate") {
      // Given
      every { noticeMutationService.updateNotice(1L, "admin", 1L, any()) } returns notice()
      val input = UpdateNoticeInput(content = "本周六凌晨维护", title = "系统维护通知", type = GraphQLNoticeType.PLATFORM_NOTICE)

      // When
      val result = resolver.updateNotice(1L, input, principal())

      // Then
      result.title shouldBe "系统维护通知"
      verify { noticeMutationService.updateNotice(1L, "admin", 1L, any()) }
    }
  })
