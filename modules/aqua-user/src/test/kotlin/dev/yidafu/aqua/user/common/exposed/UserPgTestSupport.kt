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

package dev.yidafu.aqua.user.common.exposed

import dev.yidafu.aqua.common.domain.table.AddressesTable
import dev.yidafu.aqua.common.domain.table.AdminsTable
import dev.yidafu.aqua.common.domain.table.NotificationSettingsTable
import dev.yidafu.aqua.common.domain.table.RegionsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * user 仓储实现测试的 Testcontainers 真实 PG 支撑(JPA→Exposed 迁移 P3 收尾,覆盖率门禁配套),
 * 模式与 analytics 的 AnalyticsPgTestSupport 一致。
 */
object UserPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquauserrepo")

  private val allTables: Array<Table> =
    arrayOf(UsersTable, AddressesTable, AdminsTable, RegionsTable, NotificationSettingsTable)

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(UserPgTestSupport.database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }

  private var nextId = 70000L

  fun id(): Long = nextId++

  fun user(
    id: Long = id(),
    openId: String = "openid-$id",
    phone: String? = "1380000$id",
    nickname: String? = "用户$id",
    status: String = "ACTIVE",
    email: String = "u$id@t.cn",
    customerGroupId: Long? = null,
    totalSpentCents: Long = 0L,
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(UserPgTestSupport.database) {
      UsersTable.insert {
        it[UsersTable.id] = id
        it[wechatOpenid] = openId
        it[UsersTable.phone] = phone
        it[UsersTable.nickname] = nickname
        it[UsersTable.email] = email
        it[UsersTable.status] = status
        it[role] = "USER"
        it[UsersTable.totalSpentCents] = totalSpentCents
        it[UsersTable.customerGroupId] = customerGroupId
        it[UsersTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[lastLoginAt] = createdAt
        it[isDeleted] = deleted
      }
      id
    }

  fun admin(
    id: Long = id(),
    username: String = "w$id",
    phone: String? = "1390000$id",
    role: String = "DELIVERY_WORKER",
    userId: Long? = null,
    onlineStatus: String = "OFFLINE",
    deleted: Boolean = false,
  ): Long =
    transaction(UserPgTestSupport.database) {
      AdminsTable.insert {
        it[AdminsTable.id] = id
        it[AdminsTable.username] = username
        it[passwordHash] = "x"
        it[AdminsTable.phone] = phone
        it[AdminsTable.role] = role
        it[AdminsTable.userId] = userId
        it[accountStatus] = "ACTIVE"
        it[AdminsTable.onlineStatus] = onlineStatus
        it[totalOrders] = 0
        it[completedOrders] = 0
        it[isAvailable] = true
        it[AdminsTable.createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
        it[isDeleted] = deleted
      }
      id
    }

  fun address(
    id: Long = id(),
    userId: Long,
    isDefault: Boolean = false,
    receiverName: String = "张三$id",
    phone: String = "1380000$id",
    detailAddress: String = "路$id 号",
    longitude: Double? = null,
    latitude: Double? = null,
    deleted: Boolean = false,
  ): Long =
    transaction(UserPgTestSupport.database) {
      AddressesTable.insert {
        it[AddressesTable.id] = id
        it[AddressesTable.userId] = userId
        it[AddressesTable.receiverName] = receiverName
        it[AddressesTable.phone] = phone
        it[province] = "浙江省"
        it[provinceCode] = "33"
        it[city] = "杭州市"
        it[cityCode] = "3301"
        it[district] = "西湖区"
        it[districtCode] = "330106"
        it[AddressesTable.detailAddress] = detailAddress
        it[AddressesTable.longitude] = longitude
        it[AddressesTable.latitude] = latitude
        it[AddressesTable.isDefault] = isDefault
        it[AddressesTable.createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
        it[isDeleted] = deleted
      }
      id
    }

  fun region(
    id: Long = id(),
    code: String = "R$id",
    name: String = "区域$id",
    parentCode: String? = "0",
    level: Int = 1,
  ): Long =
    transaction(UserPgTestSupport.database) {
      RegionsTable.insert {
        it[RegionsTable.id] = id
        it[RegionsTable.name] = name
        it[RegionsTable.code] = code
        it[RegionsTable.parentCode] = parentCode
        it[RegionsTable.level] = level
        it[RegionsTable.createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
      id
    }
}
