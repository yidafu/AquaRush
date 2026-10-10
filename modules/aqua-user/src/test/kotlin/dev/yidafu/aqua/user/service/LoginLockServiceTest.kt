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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 登录失败锁定单元测试(PRD 11-system-settings.md §F-SYS-004,AC-SYS-004-02):
 * 连续失败达阈值锁定、锁定期内拒绝、成功清零、锁定到期重置计数。
 */
class LoginLockServiceTest :
  FunSpec({
    var systemConfig: SystemConfigQueryApiService = mockk()
    var service: LoginLockService = mockk()

    beforeTest {
      systemConfig = mockk(relaxed = true)
      every { systemConfig.getLongValue(SystemSettingKeys.LOGIN_FAIL_LOCK_THRESHOLD, 5L) } returns 3L
      every { systemConfig.getLongValue(SystemSettingKeys.LOGIN_LOCK_DURATION_MINUTES, 30L) } returns 30L
      service = LoginLockService(systemConfig)
    }

    test("未失败时未锁定") {
      service.lockedRemainingSeconds("alice") shouldBe 0L
    }

    test("未达阈值不锁定") {
      service.recordFailure("alice")
      service.recordFailure("alice")

      service.lockedRemainingSeconds("alice") shouldBe 0L
    }

    test("连续失败达阈值后锁定,剩余秒数大于 0") {
      service.recordFailure("alice")
      service.recordFailure("alice")
      service.recordFailure("alice")

      service.lockedRemainingSeconds("alice") shouldBeGreaterThan 0L
    }

    test("锁定期内的失败不延长锁定") {
      service.recordFailure("alice")
      service.recordFailure("alice")
      service.recordFailure("alice")
      val remainingAfterLock = service.lockedRemainingSeconds("alice")

      service.recordFailure("alice")

      val remainingNow = service.lockedRemainingSeconds("alice")
      (remainingNow <= remainingAfterLock) shouldBe true
    }

    test("登录成功清零后重新可失败") {
      service.recordFailure("alice")
      service.recordFailure("alice")
      service.recordFailure("alice")
      service.lockedRemainingSeconds("alice") shouldBeGreaterThan 0L

      service.clear("alice")

      service.lockedRemainingSeconds("alice") shouldBe 0L
      // 清零后重新从 1 计数,不立即复锁
      service.recordFailure("alice")
      service.lockedRemainingSeconds("alice") shouldBe 0L
    }

    test("账号之间互不影响") {
      service.recordFailure("alice")
      service.recordFailure("alice")
      service.recordFailure("alice")

      service.lockedRemainingSeconds("bob") shouldBe 0L
    }
  })
