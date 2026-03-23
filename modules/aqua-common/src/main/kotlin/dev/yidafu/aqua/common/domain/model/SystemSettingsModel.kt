/*
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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 系统配置实体
 */
@Entity
@Table(name = "system_settings")
data class SystemSettingsModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "setting_key", nullable = false, unique = true)
  val settingKey: String = "",
  @Column(name = "setting_value")
  var settingValue: String? = null,
  @Column(name = "description")
  val description: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  @PrePersist
  fun onPrePersist() {
    id = DefaultIdGenerator().generate()
  }
}

/**
 * 系统配置键常量
 */
object SystemSettingKeys {
  const val BUCKET_DEPOSIT_AMOUNT = "bucket_deposit_amount" // 押桶金额（分）
}
