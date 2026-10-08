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

package dev.yidafu.aqua.logging.context

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class UserContextHolderTest :
  FunSpec({
    val context = UserContext(userId = "1", username = "alice", role = "ADMIN", tenantId = "t1")

    afterTest {
      UserContextHolder.clear()
    }
    test("set and get should round trip") {
      UserContextHolder.setUserContext(context)
      UserContextHolder.getUserContext() shouldBe context
    }
    test("get should return null when unset") {
      UserContextHolder.getUserContext().shouldBeNull()
    }
    test("getUserId should return user id") {
      UserContextHolder.setUserContext(context)
      UserContextHolder.getUserId() shouldBe "1"
    }
    test("getUsername should return username") {
      UserContextHolder.setUserContext(context)
      UserContextHolder.getUsername() shouldBe "alice"
    }
    test("getUserRole should return role") {
      UserContextHolder.setUserContext(context)
      UserContextHolder.getUserRole() shouldBe "ADMIN"
    }
    test("getUserRole should return null when role is null") {
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "alice"))
      UserContextHolder.getUserRole().shouldBeNull()
    }
    test("accessors should return null when no context") {
      UserContextHolder.getUserId().shouldBeNull()
      UserContextHolder.getUsername().shouldBeNull()
      UserContextHolder.getUserRole().shouldBeNull()
    }
    test("clear should remove context") {
      UserContextHolder.setUserContext(context)
      UserContextHolder.clear()
      UserContextHolder.getUserContext().shouldBeNull()
    }
    test("hasUserContext should reflect state") {
      UserContextHolder.hasUserContext() shouldBe false
      UserContextHolder.setUserContext(context)
      UserContextHolder.hasUserContext() shouldBe true
      UserContextHolder.clear()
      UserContextHolder.hasUserContext() shouldBe false
    }
  })
