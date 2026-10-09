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

package dev.yidafu.aqua.ops.systemconfig.domain.repository

import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.ops.common.exposed.ExposedH2TestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * 系统参数仓储实现测试（H2 + Exposed，F-SYS-001）。
 * setting_key 唯一；本表无软删列，查询不过滤 is_deleted。
 */
class SystemSettingsRepositoryTest :
  FunSpec({
    val database = ExposedH2TestSupport.database
    val repository = SystemSettingsRepository(database)

    beforeSpec { ExposedH2TestSupport.reset() }

    fun setting(
      key: String,
      value: String? = "1",
      group: SystemSettingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
    ): SystemSettingsModel =
      SystemSettingsModel(
        settingKey = key,
        settingValue = value,
        settingType = SystemSettingType.NUMBER,
        settingGroup = group,
        updatedBy = "admin",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("save 新建 + findBySettingKey 回读") {
      ExposedH2TestSupport.reset()
      repository.save(setting("low_stock_threshold", "10"))

      val loaded = repository.findBySettingKey("low_stock_threshold")
      loaded.shouldNotBeNull()
      loaded.settingValue shouldBe "10"
      loaded.settingGroup shouldBe SystemSettingGroup.ORDER_AND_FULFILLMENT
    }

    test("findBySettingKey 不存在返回 null") {
      ExposedH2TestSupport.reset()
      repository.findBySettingKey("not_exist") shouldBe null
    }

    test("save 更新：值与 updated_by 覆盖，其余列不动") {
      ExposedH2TestSupport.reset()
      val saved = repository.save(setting("login_fail_lock_threshold", "5"))
      saved.settingValue = "8"
      repository.save(saved)

      val reloaded = repository.findBySettingKey("login_fail_lock_threshold").shouldNotBeNull()
      reloaded.settingValue shouldBe "8"
      reloaded.description shouldBe null
    }

    test("findAllBySettingGroupOrderBySettingKey 组内按键升序") {
      ExposedH2TestSupport.reset()
      repository.save(setting("c_key", group = SystemSettingGroup.INVENTORY))
      repository.save(setting("a_key", group = SystemSettingGroup.INVENTORY))
      repository.save(setting("b_key", group = SystemSettingGroup.ORDER_AND_FULFILLMENT))

      val inventory = repository.findAllBySettingGroupOrderBySettingKey(SystemSettingGroup.INVENTORY)
      inventory.map { it.settingKey } shouldBe listOf("a_key", "c_key")
    }

    test("findAllBySettingKeyIn 空集合返回空列表") {
      ExposedH2TestSupport.reset()
      repository.findAllBySettingKeyIn(emptyList()) shouldBe emptyList()

      repository.save(setting("token_validity_hours", "24"))
      repository.findAllBySettingKeyIn(listOf("token_validity_hours", "missing")).map { it.settingKey } shouldBe
        listOf("token_validity_hours")
    }
  })
