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

package dev.yidafu.aqua.client.marketing.resolver

import dev.yidafu.aqua.common.graphql.scalars.BigDecimalScalar
import dev.yidafu.aqua.common.graphql.scalars.JsonArrayScalar
import dev.yidafu.aqua.common.graphql.scalars.JsonObjectScalar
import dev.yidafu.aqua.common.graphql.scalars.LocalDateTimeScalar
import dev.yidafu.aqua.common.graphql.scalars.LongScalar
import dev.yidafu.aqua.common.graphql.scalars.MapScalar
import dev.yidafu.aqua.common.graphql.scalars.PrimaryIdScalar
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.GraphQLInputObjectType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * 回归测试:小程序确认订单计价预览传入 preferredCouponId=0(不用券哨兵值,见
 * frontend/user-weapp/src/pages/order-confirm)时,GraphQL 变量校验必须放行。
 *
 * 历史缺陷:`preferredCouponId` 声明为 PrimaryId,而 PrimaryId 标量强制要求正值,
 * 在 @min(value: 0) 指令有机会校验之前,标量即抛出
 * "PrimaryId must be positive but got: 0"(ValidationError),导致下单/计价预览全部失败。
 * 修复:该字段类型改为 Long(接受 0),引擎侧 CouponRule 以 0L 表示「不用券」。
 *
 * 本测试直接加载真实 client schema 与共享类型,并以真实标量构建可执行 schema,
 * 复现用户端发出的请求,断言不再出现 PrimaryId 校验错误。
 */
class OrderPreviewPreferredCouponSchemaTest :
  FunSpec({

    val sharedTypes = readSchema("graphql/shared-types.graphqls")
    val clientSchema = readSchema("graphql/client-schema.graphqls")

    val registry =
      SchemaParser()
        .parse(sharedTypes)
        .merge(SchemaParser().parse(clientSchema))

    val wiring =
      RuntimeWiring
        .newRuntimeWiring()
        .scalar(BigDecimalScalar.GraphQL_TYPE)
        .scalar(LocalDateTimeScalar.GraphQL_TYPE)
        .scalar(LongScalar.GraphQL_TYPE)
        .scalar(MapScalar.GraphQL_TYPE)
        .scalar(JsonObjectScalar.GraphQL_TYPE)
        .scalar(JsonArrayScalar.GraphQL_TYPE)
        .scalar(PrimaryIdScalar.GraphQL_TYPE)
        .build()

    val schema = SchemaGenerator().makeExecutableSchema(registry, wiring)
    val graphQL = GraphQL.newGraphQL(schema).build()

    test("preferredCouponId 字段类型为 Long 而非 PrimaryId(允许 0 哨兵)") {
      val orderPreviewInput = schema.getType("OrderPreviewInput") as GraphQLInputObjectType
      orderPreviewInput.getField("preferredCouponId").type.typeName() shouldBe "Long"

      val createOrderInput = schema.getType("CreateOrderInput") as GraphQLInputObjectType
      createOrderInput.getField("preferredCouponId").type.typeName() shouldBe "Long"
    }

    test("orderPreview 接受 preferredCouponId=0,不再报 PrimaryId 校验错误") {
      // 复现小程序 order-confirm 发出的真实请求(preferredCouponId: 0 = 不用券)
      val query =
        """
        query OrderPreview(${'$'}input: OrderPreviewInput!) {
          orderPreview(input: ${'$'}input) {
            payAmountCents
          }
        }
        """.trimIndent()

      val variables =
        mapOf(
          "input" to
            mapOf(
              "items" to listOf(mapOf("productId" to "3000000000000000001", "quantity" to 2)),
              "addressId" to "744574089519173632",
              "preferredCouponId" to 0,
              "ticketRedeemCount" to 0,
            ),
        )

      val result =
        graphQL.execute(
          ExecutionInput
            .newExecutionInput(query)
            .variables(variables)
            .build(),
        )

      val messages = result.errors.map { it.message }.joinToString("\n")
      // 变量校验必须通过(下方断言捕获修复前“PrimaryId must be positive”的 ValidationError)
      messages.shouldNotContain("PrimaryId must be positive")
      messages.shouldNotContain("Variable 'input' has an invalid value")
    }

    test("PrimaryId 标量仍拒绝 0(实体主键语义不受影响)") {
      val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
      val ex =
        kotlin
          .runCatching { coercing.parseValue(0L) }
          .exceptionOrNull()
      ex?.message shouldContain "PrimaryId must be positive"
    }
  })

/** 从测试 classpath 读取 schema 文件内容(client 的 main resources 含 graphql/ 与 shared-config 软链) */
private fun readSchema(path: String): String =
  checkNotNull(
    OrderPreviewPreferredCouponSchemaTest::class.java.classLoader.getResourceAsStream(path),
  ) { "schema resource not found: $path" }
    .bufferedReader(Charsets.UTF_8)
    .use { it.readText() }

/** 取输入字段类型名(去除 NonNull 包装) */
private fun graphql.schema.GraphQLInputType.typeName(): String =
  (graphql.schema.GraphQLTypeUtil.unwrapNonNull(this) as graphql.schema.GraphQLNamedType).name
