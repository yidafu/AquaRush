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

import dev.yidafu.aqua.common.domain.model.UserSubscriptionAuthModel
import dev.yidafu.aqua.common.exposed.PlatformPgTestSupport
import dev.yidafu.aqua.common.exposed.PlatformPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class UserSubscriptionAuthRepositoryImplTest :
  FunSpec({
    val repo = UserSubscriptionAuthRepositoryImpl(PlatformPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增/更新与查询") {
      val saved = repo.save(UserSubscriptionAuthModel(userId = 1L, templateId = "tpl-1"))
      saved.id.shouldNotBeNull()

      repo.findByUserIdAndTemplateId(1L, "tpl-1").orElse(null).shouldNotBeNull()
      repo.findByUserIdAndTemplateId(1L, "tpl-x").isEmpty shouldBe true
      repo.findByUserId(1L) shouldHaveSize 1

      repo.save(saved.apply { remainingCount = 0 })
      repo
        .findByUserIdAndTemplateId(1L, "tpl-1")
        .orElse(null)
        .shouldNotBeNull()
        .remainingCount shouldBe 0
    }
  })
