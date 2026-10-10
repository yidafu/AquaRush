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

package dev.yidafu.aqua.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.common.exposed.PlatformPgTestSupport
import dev.yidafu.aqua.common.exposed.PlatformPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class UserNotificationSettingsRepositoryImplTest :
  FunSpec({
    val repo = UserNotificationSettingsRepositoryImpl(PlatformPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增/更新、findByUserId/existsByUserId 与软删") {
      val saved = repo.save(UserNotificationSettingsModel(userId = 1L))
      saved.id.shouldNotBeNull()
      repo.existsByUserId(1L) shouldBe true

      repo.save(saved.apply { promotionalNotifications = true })
      repo
        .findByUserId(1L)
        .orElse(null)
        .shouldNotBeNull()
        .promotionalNotifications shouldBe true

      repo.deleteByUserId(1L) shouldBe 1
      repo.existsByUserId(1L) shouldBe false
      repo.deleteByUserId(1L) shouldBe 0
    }
  })
