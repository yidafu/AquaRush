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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.user.common.exposed.UserExposedH2TestSupport
import dev.yidafu.aqua.user.common.exposed.UserExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class RegionRepositoryImplTest :
  FunSpec({
    val repo = RegionRepositoryImpl(UserExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save/查询/软删/exists") {
      val saved = repo.save(RegionModel(name = "杭州", code = "HZ", parentCode = "ZJ", level = 2))
      saved.id.shouldNotBeNull()

      repo.findByCode("HZ").shouldNotBeNull()
      repo.existsByCode("HZ") shouldBe true
      repo.existsByCodeAndLevel("HZ", 2) shouldBe true
      repo.existsByCodeAndLevel("HZ", 3) shouldBe false
      repo.findById(saved.id!!).shouldNotBeNull()
      repo.findAll() shouldHaveSize 1

      repo.delete(saved)
      repo.findByCode("HZ") shouldBe null
    }

    test("层级/父级/名称过滤查询") {
      UserExposedH2TestSupport.region(code = "R1", name = "浙江省", parentCode = "0", level = 1)
      UserExposedH2TestSupport.region(code = "R2", name = "杭州市", parentCode = "R1", level = 2)
      UserExposedH2TestSupport.region(code = "R3", name = "宁波市", parentCode = "R1", level = 2)

      repo.findRootRegions(1) shouldHaveSize 1
      repo.findByParentCodeAndLevel("R1", 2) shouldHaveSize 2
      repo.findByLevel(2) shouldHaveSize 2
      repo.findByLevelOrderByCode(1) shouldHaveSize 1
      repo.findByParentCodeOrderByCode("R1") shouldHaveSize 2
      repo.findByNameContainingAndLevelOrderByCode("州", 2) shouldHaveSize 1 // 仅杭州市含"州"
      repo.findByNameContainingOrderByCode("浙江") shouldHaveSize 1
      repo.existsByNameAndLevelAndParentCode("杭州市", 2, "R1") shouldBe true
      repo.existsByNameAndLevelAndParentCode("杭州市", 3, "R1") shouldBe false
    }

    test("save 更新分支") {
      val id = UserExposedH2TestSupport.region()
      val loaded = repo.findById(id).shouldNotBeNull()
      repo.save(loaded) // RegionModel 业务列全为 val,update 分支以原值写回
      repo.findById(id).shouldNotBeNull().name shouldBe loaded.name
    }
  })
