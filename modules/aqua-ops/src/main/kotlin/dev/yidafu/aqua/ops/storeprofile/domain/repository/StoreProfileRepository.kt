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

package dev.yidafu.aqua.ops.storeprofile.domain.repository

import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.ops.storeprofile.domain.table.StoreProfileTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * 店铺信息仓库（Exposed 实现，单行配置固定 id = [SINGLETON_ID]，F-OPS-004）。
 *
 * 软删过滤显式带上（原 `@SoftDelete` 由 Hibernate 自动追加）；
 * `save` 更新时显式刷新 `updated_at`（对齐实体 `@PreUpdate`）。
 */
@Repository
class StoreProfileRepository(
  private val database: Database,
) {
  companion object {
    /** 单行配置固定主键 */
    const val SINGLETON_ID = 1L
  }

  fun findById(id: Long): Optional<StoreProfileModel> =
    transaction(database) {
      StoreProfileTable
        .selectAll()
        .where { (StoreProfileTable.id eq id) and (StoreProfileTable.isDeleted eq false) }
        .singleOrNull()
        ?.let(::toModel)
    }.let { Optional.ofNullable(it) }

  fun save(profile: StoreProfileModel): StoreProfileModel =
    transaction(database) {
      // 本表主键由服务层显式指定(固定 id=1)，与 JPA 期一致：无主键无法落库
      val id =
        profile.id
          ?: error("StoreProfileModel.id 必须在保存前赋值(单行配置固定 id=${SINGLETON_ID})")
      val exists =
        StoreProfileTable
          .selectAll()
          .where { StoreProfileTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        val now = java.time.LocalDateTime.now()
        StoreProfileTable.update({ StoreProfileTable.id eq id }) {
          it[shopName] = profile.shopName
          it[logoUrl] = profile.logoUrl
          it[serviceHotline] = profile.serviceHotline
          it[businessHoursStart] = profile.businessHoursStart
          it[businessHoursEnd] = profile.businessHoursEnd
          it[shopIntro] = profile.shopIntro
          it[aboutUs] = profile.aboutUs
          it[deletedAt] = profile.deletedAt
          it[deletedBy] = profile.deletedBy
          it[updatedAt] = now
        }
        profile.updatedAt = now
      } else {
        StoreProfileTable.insert {
          it[StoreProfileTable.id] = id
          it[isDeleted] = false // 显式写入：Exposed 的 default() 不会进 INSERT 语句，而 DDL 的 NOT NULL 无兜底
          it[shopName] = profile.shopName
          it[logoUrl] = profile.logoUrl
          it[serviceHotline] = profile.serviceHotline
          it[businessHoursStart] = profile.businessHoursStart
          it[businessHoursEnd] = profile.businessHoursEnd
          it[shopIntro] = profile.shopIntro
          it[aboutUs] = profile.aboutUs
          it[deletedAt] = profile.deletedAt
          it[deletedBy] = profile.deletedBy
          it[createdAt] = profile.createdAt
          it[updatedAt] = profile.updatedAt
        }
      }
      profile
    }

  /**
   * 原生硬删除（绕过软删除）：软删行仍占用 SINGLETON_ID，重新保存会撞主键（测试复位用）。
   * Exposed 的 `deleteAll` 天然是物理删除，无需再写原生 SQL（见 DeliveryFeeRuleRepository.hardDeleteAll）。
   */
  fun hardDeleteAll() {
    transaction(database) {
      StoreProfileTable.deleteAll()
    }
  }

  private fun toModel(row: ResultRow): StoreProfileModel =
    StoreProfileModel(
      id = row[StoreProfileTable.id],
      shopName = row[StoreProfileTable.shopName],
      logoUrl = row[StoreProfileTable.logoUrl],
      serviceHotline = row[StoreProfileTable.serviceHotline],
      businessHoursStart = row[StoreProfileTable.businessHoursStart],
      businessHoursEnd = row[StoreProfileTable.businessHoursEnd],
      shopIntro = row[StoreProfileTable.shopIntro],
      aboutUs = row[StoreProfileTable.aboutUs],
      deletedAt = row[StoreProfileTable.deletedAt],
      deletedBy = row[StoreProfileTable.deletedBy],
      createdAt = row[StoreProfileTable.createdAt],
      updatedAt = row[StoreProfileTable.updatedAt],
    )
}
