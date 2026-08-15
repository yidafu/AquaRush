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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import dev.yidafu.aqua.common.graphql.generated.NoticeType as GraphQLNoticeType

class AdminNoticeMutationResolverTest {
  private lateinit var noticeMutationService: NoticeMutationService
  private lateinit var resolver: AdminNoticeMutationResolver

  private fun notice(): NoticeModel =
    NoticeModel(
      id = 1L,
      title = "系统维护通知",
      content = "本周六凌晨维护",
      type = NoticeType.PLATFORM_NOTICE,
      status = NoticeStatus.PUBLISHED,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    noticeMutationService = mockk(relaxed = true)
    resolver = AdminNoticeMutationResolver(noticeMutationService)
  }

  @Test
  fun `createNotice should delegate`() {
    // Given
    every { noticeMutationService.createNotice(1L, "admin", any()) } returns notice()
    val input = CreateNoticeInput(content = "本周六凌晨维护", title = "系统维护通知", type = GraphQLNoticeType.PLATFORM_NOTICE)

    // When
    val result = resolver.createNotice(input, principal())

    // Then
    assertEquals("系统维护通知", result.title)
    verify { noticeMutationService.createNotice(1L, "admin", any()) }
  }

  @Test
  fun `publishNotice should delegate`() {
    // Given
    every { noticeMutationService.publishNotice(1L, "admin", 1L) } returns notice()

    // When
    val result = resolver.publishNotice(1L, principal())

    // Then
    assertEquals("系统维护通知", result.title)
    verify { noticeMutationService.publishNotice(1L, "admin", 1L) }
  }

  @Test
  fun `stopNotice should delegate`() {
    // Given
    every { noticeMutationService.stopNotice(1L, "admin", 1L) } returns notice()

    // When
    val result = resolver.stopNotice(1L, principal())

    // Then
    assertEquals(NoticeStatus.PUBLISHED, NoticeStatus.valueOf(result.status.name))
    verify { noticeMutationService.stopNotice(1L, "admin", 1L) }
  }

  @Test
  fun `setNoticePinned should delegate`() {
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
    assertEquals(true, result.pinned)
    verify { noticeMutationService.setNoticePinned(1L, "admin", 1L, true) }
  }

  @Test
  fun `updateNotice should delegate`() {
    // Given
    every { noticeMutationService.updateNotice(1L, "admin", 1L, any()) } returns notice()
    val input = UpdateNoticeInput(content = "本周六凌晨维护", title = "系统维护通知", type = GraphQLNoticeType.PLATFORM_NOTICE)

    // When
    val result = resolver.updateNotice(1L, input, principal())

    // Then
    assertEquals("系统维护通知", result.title)
    verify { noticeMutationService.updateNotice(1L, "admin", 1L, any()) }
  }
}
