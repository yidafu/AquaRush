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

package dev.yidafu.aqua.logging.service

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

class UserActionLogServiceTest {
  private lateinit var userActionLogRepository: UserActionLogRepository
  private lateinit var service: UserActionLogService

  @BeforeEach
  fun setUp() {
    userActionLogRepository = mockk(relaxed = true)
    // relaxed mock 对泛型返回(save 返回 S)会生成 Object 导致 ClassCastException,需显式 stub
    every { userActionLogRepository.save(any()) } answers { firstArg() }
    service = UserActionLogService(userActionLogRepository)
  }

  @Test
  fun `saveUserActionLogSync should persist log`() {
    service.saveUserActionLogSync(
      userId = "1",
      username = "u",
      actionType = "CLICK",
      target = "btn",
      pageUrl = "/home",
      elementId = "e",
      elementType = "button",
      elementText = "确定",
      clientIp = "127.0.0.1",
      userAgent = "ua",
      properties = "{}",
    )
    verify { userActionLogRepository.save(any<UserActionLogModel>()) }
  }

  @Test
  fun `saveUserActionLog should persist log with defaults`() {
    service.saveUserActionLog(actionType = "VIEW", target = "/home")
    verify { userActionLogRepository.save(any<UserActionLogModel>()) }
  }

  @Test
  fun `saveUserActionLog should swallow repository exceptions`() {
    every { userActionLogRepository.save(any<UserActionLogModel>()) } throws RuntimeException("db down")
    service.saveUserActionLog(actionType = "VIEW", target = "/home")
  }

  @Test
  fun `queryUserActionLogs should delegate to repository`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(listOf(UserActionLogModel(actionType = "CLICK")), pageable, 1)
    every {
      userActionLogRepository.findByFilters(any(), any(), any(), any(), any(), any(), any())
    } returns page
    val result = service.queryUserActionLogs("1", "CLICK", "u", "btn", null, null, pageable)
    assertEquals(1, result.totalElements)
  }

  @Test
  fun `findByUserId should delegate to repository`() {
    every { userActionLogRepository.findByUserId("1") } returns listOf(UserActionLogModel(userId = "1"))
    assertEquals(1, service.findByUserId("1").size)
  }

  @Test
  fun `findByActionType should delegate to repository`() {
    every { userActionLogRepository.findByActionType("CLICK") } returns listOf(UserActionLogModel(actionType = "CLICK"))
    assertEquals(1, service.findByActionType("CLICK").size)
  }
}
