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

package dev.yidafu.aqua.admin.db

import dev.yidafu.aqua.admin.AquaAdminApplication
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

/**
 * 坐标体系统一(v1.6.0)迁移测试:
 * 存量 addresses / delivery_addresses 的 GCJ-02 坐标 → WGS-84,
 * SQL 算法须与 frontend/common/src/utils/coordinate.ts gcj02ToWgs84 一致。
 *
 * 参考向量(由前端算法计算):
 * - 上海 (121.531541859215, 31.17530398364597) → (121.527225, 31.177440)
 * - 杭州 (120.16142, 30.259224)               → (120.156745, 30.261566)
 * - 境外 (13.405, 52.52)                      → 原样保留(WHERE 范围排除)
 */
@SpringBootTest(
  classes = [AquaAdminApplication::class],
  properties = [
    // 本测试仅验证数据迁移(DB + Liquibase),排除 GraphQL 自动配置,
    // 避免加载全局 schema(当前仓库存在跨模块重复类型定义的既有问题)
    "spring.autoconfigure.exclude=" +
      "org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration," +
      "org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration",
  ],
)
@ActiveProfiles("test")
@ApplyExtension(SpringExtension::class)
class AddressCoordinateMigrationTest(
  jdbc: JdbcTemplate,
) : FunSpec({

    // 从 classpath 加载迁移 SQL(与 Liquibase changeSet 同一文件),按 ';' 拆分逐条执行
    val migrationStatements =
      javaClass
        .getResource("/db/changelog/changes/sql/1.6.0-address-coords-gcj-to-wgs84.sql")!!
        .readText()
        .lineSequence()
        .filterNot { it.trimStart().startsWith("--") }
        .joinToString("\n")
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val tolerance = 0.0002 // ~20m,单步逆变换残差 ~1m,足够排除 ~500m 级 GCJ 偏移

    fun insertAddress(
      id: Long,
      longitude: Double,
      latitude: Double,
    ) {
      jdbc.update(
        """
        INSERT INTO addresses
          (id, user_id, receiver_name, phone, province, city, district, detail_address,
           longitude, latitude, is_default, is_deleted, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """.trimIndent(),
        id,
        1000000000000000011L,
        "迁移测试",
        "13800138000",
        "浙江",
        "杭州",
        "西湖区",
        "测试地址",
        longitude,
        latitude,
        false,
        false,
      )
    }

    fun readAddress(id: Long): Pair<Double, Double> {
      val row = jdbc.queryForMap("SELECT longitude, latitude FROM addresses WHERE id = ?", id)
      return (row["longitude"] as Number).toDouble() to (row["latitude"] as Number).toDouble()
    }

    context("地址坐标 GCJ-02 → WGS-84 迁移(v1.6.0)") {
      test("addresses 上海向量转换与前端算法一致") {
        insertAddress(9000000000000000001L, 121.531541859215, 31.17530398364597)
        migrationStatements.forEach { jdbc.execute(it) }

        val (lng, lat) = readAddress(9000000000000000001L)
        lng shouldBe (121.527225 plusOrMinus tolerance)
        lat shouldBe (31.177440 plusOrMinus tolerance)
      }

      test("addresses 杭州测试数据向量转换与前端算法一致") {
        insertAddress(9000000000000000002L, 120.16142, 30.259224)
        migrationStatements.forEach { jdbc.execute(it) }

        val (lng, lat) = readAddress(9000000000000000002L)
        lng shouldBe (120.156745 plusOrMinus tolerance)
        lat shouldBe (30.261566 plusOrMinus tolerance)
      }

      test("境外坐标不在迁移范围内,原样保留") {
        insertAddress(9000000000000000003L, 13.405, 52.52)
        migrationStatements.forEach { jdbc.execute(it) }

        val (lng, lat) = readAddress(9000000000000000003L)
        lng shouldBe (13.405 plusOrMinus tolerance)
        lat shouldBe (52.52 plusOrMinus tolerance)
      }

      test("delivery_addresses 快照坐标同样转换") {
        jdbc.update(
          """
          INSERT INTO delivery_addresses
            (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address,
             longitude, latitude, is_deleted, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """.trimIndent(),
          9000000000000000001L,
          1000000000000000011L,
          "迁移测试",
          "13800138000",
          "浙江",
          "杭州",
          "西湖区",
          "测试地址",
          121.531541859215,
          31.17530398364597,
          false,
        )
        migrationStatements.forEach { jdbc.execute(it) }

        val row =
          jdbc.queryForMap(
            "SELECT longitude, latitude FROM delivery_addresses WHERE id = ?",
            9000000000000000001L,
          )
        val lng = (row["longitude"] as Number).toDouble()
        val lat = (row["latitude"] as Number).toDouble()
        lng shouldBe (121.527225 plusOrMinus tolerance)
        lat shouldBe (31.177440 plusOrMinus tolerance)
        (121.531541859215 - lng) shouldBeGreaterThan 0.001 // 坐标应发生 GCJ 级偏移转换
      }
    }
  })
