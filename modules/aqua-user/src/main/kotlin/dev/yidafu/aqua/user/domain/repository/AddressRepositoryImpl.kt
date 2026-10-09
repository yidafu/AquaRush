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

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.table.AddressesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 收货地址仓储实现(JPA→Exposed 迁移 P3 收尾)。
 * findNearby 保持 Haversine 原生 SQL(与原 JPA nativeQuery 逐字等价,H2/PostgreSQL 兼容)。
 */
@Repository
class AddressRepositoryImpl(
  private val database: Database,
) : AddressRepository {
  private fun toModel(row: ResultRow): AddressModel =
    AddressModel(
      id = row[AddressesTable.id],
      userId = row[AddressesTable.userId],
      receiverName = row[AddressesTable.receiverName],
      phone = row[AddressesTable.phone],
      province = row[AddressesTable.province],
      provinceCode = row[AddressesTable.provinceCode] ?: "",
      city = row[AddressesTable.city],
      cityCode = row[AddressesTable.cityCode] ?: "",
      district = row[AddressesTable.district],
      districtCode = row[AddressesTable.districtCode] ?: "",
      detailAddress = row[AddressesTable.detailAddress],
      floor = row[AddressesTable.floor],
      longitude = row[AddressesTable.longitude],
      latitude = row[AddressesTable.latitude],
      isDefault = row[AddressesTable.isDefault],
      createdAt = row[AddressesTable.createdAt],
      updatedAt = row[AddressesTable.updatedAt],
    )

  private fun selectAllWhere(condition: () -> Op<Boolean>) = AddressesTable.selectAll().where { AddressesTable.activeOnly(condition) }

  override fun findById(id: Long): Optional<AddressModel> =
    Optional.ofNullable(
      transaction(database) {
        selectAllWhere { AddressesTable.id eq id }.limit(1).firstOrNull()
      }?.let(::toModel),
    )

  override fun existsById(id: Long): Boolean = findById(id).isPresent

  override fun deleteById(id: Long) {
    transaction(database) {
      val now = LocalDateTime.now()
      AddressesTable.update({ AddressesTable.id eq id }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun delete(entity: AddressModel) {
    val id = requireNotNull(entity.id) { "AddressModel.id 不能为空(软删必须携带主键)" }
    deleteById(id)
  }

  override fun save(entity: AddressModel): AddressModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        AddressesTable
          .selectAll()
          .where { AddressesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        AddressesTable.update({ AddressesTable.id eq id }) {
          it[receiverName] = entity.receiverName
          it[AddressesTable.phone] = entity.phone
          it[province] = entity.province
          it[provinceCode] = entity.provinceCode
          it[city] = entity.city
          it[cityCode] = entity.cityCode
          it[district] = entity.district
          it[districtCode] = entity.districtCode
          it[detailAddress] = entity.detailAddress
          it[floor] = entity.floor
          it[longitude] = entity.longitude
          it[latitude] = entity.latitude
          it[isDefault] = entity.isDefault
          it[updatedAt] = now
        }
      } else {
        AddressesTable.insert {
          it[AddressesTable.id] = id
          it[AddressesTable.userId] = entity.userId
          it[AddressesTable.receiverName] = entity.receiverName
          it[AddressesTable.phone] = entity.phone
          it[AddressesTable.province] = entity.province
          it[AddressesTable.provinceCode] = entity.provinceCode
          it[AddressesTable.city] = entity.city
          it[AddressesTable.cityCode] = entity.cityCode
          it[AddressesTable.district] = entity.district
          it[AddressesTable.districtCode] = entity.districtCode
          it[AddressesTable.detailAddress] = entity.detailAddress
          it[AddressesTable.floor] = entity.floor
          it[AddressesTable.longitude] = entity.longitude
          it[AddressesTable.latitude] = entity.latitude
          it[AddressesTable.isDefault] = entity.isDefault
          it[AddressesTable.createdAt] = entity.createdAt
          it[AddressesTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun saveAll(entities: List<AddressModel>): List<AddressModel> {
    entities.forEach { save(it) }
    return entities
  }

  override fun findAll(): List<AddressModel> =
    transaction(database) {
      selectAllWhere { Op.TRUE }.orderBy(AddressesTable.id to SortOrder.ASC).toList()
    }.map(::toModel)

  override fun findByUserId(userId: Long): List<AddressModel> =
    transaction(database) {
      selectAllWhere { AddressesTable.userId eq userId }.orderBy(AddressesTable.id to SortOrder.ASC).toList()
    }.map(::toModel)

  override fun findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId: Long): List<AddressModel> =
    transaction(database) {
      selectAllWhere { AddressesTable.userId eq userId }
        .orderBy(
          AddressesTable.isDefault to SortOrder.DESC,
          AddressesTable.createdAt to SortOrder.DESC,
        ).toList()
    }.map(::toModel)

  override fun findByUserIdOrderByIdDesc(userId: Long): List<AddressModel> =
    transaction(database) {
      selectAllWhere { AddressesTable.userId eq userId }.orderBy(AddressesTable.id to SortOrder.DESC).toList()
    }.map(::toModel)

  override fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): AddressModel? =
    transaction(database) {
      selectAllWhere {
        (AddressesTable.userId eq userId) and (AddressesTable.isDefault eq isDefault)
      }.limit(1).firstOrNull()
    }?.let(::toModel)

  override fun findByUserIdAndIsDefaultTrue(userId: Long): AddressModel? = findByUserIdAndIsDefault(userId, true)

  override fun findByUserIdAndIdNot(
    userId: Long,
    addressId: Long,
  ): List<AddressModel> =
    transaction(database) {
      selectAllWhere {
        (AddressesTable.userId eq userId) and (AddressesTable.id neq addressId)
      }.toList()
    }.map(::toModel)

  override fun countByUserId(userId: Long): Int =
    transaction(database) {
      selectAllWhere { AddressesTable.userId eq userId }.count()
    }.toInt()

  fun deleteByIdAndUserId(
    id: Long,
    userId: Long,
  ): Int =
    transaction(database) {
      val row =
        selectAllWhere { AddressesTable.id eq id }.limit(1).firstOrNull() ?: return@transaction 0
      if (row[AddressesTable.userId] != userId) {
        return@transaction 0
      }
      val now = LocalDateTime.now()
      AddressesTable.update({ AddressesTable.id eq id }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
      1
    }

  fun clearDefaultByUserId(userId: Long) {
    clearDefaultAddresses(userId)
  }

  // ===== AddressRepositoryCustom =====

  override fun clearDefaultAddresses(userId: Long): Int =
    transaction(database) {
      AddressesTable.update(
        {
          AddressesTable.activeOnly { (AddressesTable.userId eq userId) and (AddressesTable.isDefault eq true) }
        },
      ) {
        it[isDefault] = false
        it[updatedAt] = LocalDateTime.now()
      }
    }

  override fun findNearby(
    longitude: Double,
    latitude: Double,
    radiusKm: Double,
  ): List<AddressModel> {
    // Haversine 公式与原 JPA nativeQuery 逐字等价(H2 与 PostgreSQL 均支持 RADIANS/ACOS)
    val distanceExpr =
      "(6371 * acos(cos(radians(?)) * cos(radians(a.latitude)) * cos(radians(a.longitude) - radians(?)) + sin(radians(?)) * sin(radians(a.latitude))))"
    val sql =
      """
      SELECT a.* FROM addresses a
      WHERE a.longitude IS NOT NULL
        AND a.latitude IS NOT NULL
        AND a.is_deleted = false
        AND $distanceExpr <= ?
      ORDER BY $distanceExpr
      LIMIT 20
      """.trimIndent()
    val args =
      listOf(
        DoubleColumnType() to latitude,
        DoubleColumnType() to longitude,
        DoubleColumnType() to latitude,
        DoubleColumnType() to radiusKm,
        DoubleColumnType() to latitude,
        DoubleColumnType() to longitude,
        DoubleColumnType() to latitude,
      )
    return transaction(database) {
      exec(sql, args) { rs ->
        val result = mutableListOf<AddressModel>()
        while (rs.next()) {
          result +=
            AddressModel(
              id = rs.getLong("id"),
              userId = rs.getLong("user_id"),
              receiverName = rs.getString("receiver_name"),
              phone = rs.getString("phone"),
              province = rs.getString("province"),
              provinceCode = rs.getString("province_code") ?: "",
              city = rs.getString("city"),
              cityCode = rs.getString("city_code") ?: "",
              district = rs.getString("district"),
              districtCode = rs.getString("district_code") ?: "",
              detailAddress = rs.getString("detail_address"),
              floor = rs.getObject("floor") as Int?,
              longitude = rs.getObject("longitude") as Double?,
              latitude = rs.getObject("latitude") as Double?,
              isDefault = rs.getBoolean("is_default"),
              createdAt = rs.getObject("created_at") as LocalDateTime,
              updatedAt = rs.getObject("updated_at") as LocalDateTime,
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
  ): List<AddressModel> {
    val lower = "%${keyword.lowercase()}%"
    return transaction(database) {
      selectAllWhere {
        (AddressesTable.userId eq userId) and
          (
            (AddressesTable.province.lowerCase() like lower) or
              (AddressesTable.city.lowerCase() like lower) or
              (AddressesTable.district.lowerCase() like lower) or
              (AddressesTable.detailAddress.lowerCase() like lower)
          )
      }.toList()
    }.map(::toModel)
  }

  override fun searchByUserIdAndKeyword(
    userId: Long,
    keyword: String,
    pageable: Pageable,
  ): Page<AddressModel> {
    val lower = "%${keyword.lowercase()}%"
    val condition: Op<Boolean> =
      (AddressesTable.userId eq userId) and
        (
          (AddressesTable.detailAddress.lowerCase() like lower) or
            (AddressesTable.phone.lowerCase() like lower) or
            (AddressesTable.receiverName.lowerCase() like lower)
        )
    val (content, total) =
      transaction(database) {
        val content =
          selectAllWhere { condition }
            .orderBy(AddressesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to selectAllWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun searchAllAddresses(
    keyword: String?,
    pageable: Pageable,
  ): Page<AddressModel> {
    val lower = keyword?.lowercase()?.takeIf { it.isNotBlank() }?.let { "%$it%" }
    val condition: Op<Boolean>? =
      lower?.let {
        (AddressesTable.detailAddress.lowerCase() like it) or
          (AddressesTable.phone.lowerCase() like it) or
          (AddressesTable.receiverName.lowerCase() like it)
      }
    val (content, total) =
      transaction(database) {
        val content =
          selectAllWhere { condition ?: Op.TRUE }
            .orderBy(AddressesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to selectAllWhere { condition ?: Op.TRUE }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
