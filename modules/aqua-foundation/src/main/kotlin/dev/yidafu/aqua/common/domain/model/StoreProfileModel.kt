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

package dev.yidafu.aqua.common.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 店铺信息配置实体(单行配置,固定 id = 1)
 * 与 PRD 12-operations-config.md §F-OPS-004 保持一致:
 * 店铺名称、LOGO、客服电话、营业时间、店铺简介、关于我们;未配置时用户端按默认值兜底。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "store_profiles")
open class StoreProfileModel(
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "shop_name", nullable = false, length = 20)
  var shopName: String = "",
  @Column(name = "logo_url", length = 1000)
  var logoUrl: String? = null,
  @Column(name = "service_hotline", nullable = false, length = 30)
  var serviceHotline: String = "",
  @Column(name = "business_hours_start")
  var businessHoursStart: LocalTime? = null,
  @Column(name = "business_hours_end")
  var businessHoursEnd: LocalTime? = null,
  @Column(name = "shop_intro", length = 100)
  var shopIntro: String? = null,
  @Column(name = "about_us", columnDefinition = "text")
  var aboutUs: String? = null,
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
