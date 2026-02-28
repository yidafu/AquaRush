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

package dev.yidafu.aqua.common.graphql.validation

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValidationDirectivesTest {
  @Test
  fun `test notBlank directive validation`() {
    val schema = buildTestSchema()
    val graphQL = GraphQL.newGraphQL(schema).build()

    // 测试有效的输入
    val validInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { name: \"valid name\" }) }")
        .build()

    val validResult = graphQL.execute(validInput)
    assertEquals(null, validResult.errors.firstOrNull())

    // 测试无效的输入（空字符串）
    val invalidInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { name: \"\" }) }")
        .build()

    val invalidResult = graphQL.execute(invalidInput)
    assertEquals(true, invalidResult.errors.isNotEmpty())
    assertEquals("字段 name 不能为空", invalidResult.errors.first().message)
  }

  @Test
  fun `test size directive validation`() {
    val schema = buildTestSchema()
    val graphQL = GraphQL.newGraphQL(schema).build()

    // 测试有效的输入（长度在范围内）
    val validInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { name: \"valid name\" }) }")
        .build()

    val validResult = graphQL.execute(validInput)
    assertEquals(null, validResult.errors.firstOrNull())

    // 测试无效的输入（长度超过最大值）
    val invalidInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { name: \"${"a".repeat(30)}\" }) }")
        .build()

    val invalidResult = graphQL.execute(invalidInput)
    assertEquals(true, invalidResult.errors.isNotEmpty())
    assertEquals("字段 name 的长度不能超过20个字符", invalidResult.errors.first().message)
  }

  @Test
  fun `test pattern directive validation`() {
    val schema = buildTestSchema()
    val graphQL = GraphQL.newGraphQL(schema).build()

    // 测试有效的输入（符合手机号格式）
    val validInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { phone: \"13812345678\" }) }")
        .build()

    val validResult = graphQL.execute(validInput)
    assertEquals(null, validResult.errors.firstOrNull())

    // 测试无效的输入（不符合手机号格式）
    val invalidInput =
      ExecutionInput.newExecutionInput()
        .query("mutation { test(input: { phone: \"123\" }) }")
        .build()

    val invalidResult = graphQL.execute(invalidInput)
    assertEquals(true, invalidResult.errors.isNotEmpty())
    assertEquals("字段 phone 的格式不符合要求", invalidResult.errors.first().message)
  }

  @Test
  fun `test ValidationException creation`() {
    val exception = ValidationException("Test validation error")
    assertEquals("Test validation error", exception.message)
  }

  private fun buildTestSchema(): GraphQLSchema {
    val schemaDefinition = """
      directive @notBlank(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
      directive @size(min: Int, max: Int, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
      directive @pattern(regexp: String!, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION

      type Query {
        hello: String
      }

      type Mutation {
        test(input: TestInput!): String
      }

      input TestInput {
        name: String! @notBlank(message: "字段 name 不能为空") @size(max: 20, message: "字段 name 的长度不能超过20个字符")
        phone: String @pattern(regexp: "^1[3-9]\\d{9}$", message: "字段 phone 的格式不符合要求")
      }
    """

    val schemaParser = SchemaParser()
    val typeDefinitionRegistry = schemaParser.parse(schemaDefinition)

    val validationHandler = ValidationDirectiveHandler()
    val runtimeWiring =
      RuntimeWiring.newRuntimeWiring()
        .directive("notBlank", validationHandler)
        .directive("size", validationHandler)
        .directive("pattern", validationHandler)
        .type("Query") { builder ->
          builder.dataFetcher("hello") { env -> "Hello World" }
        }
        .type("Mutation") { builder ->
          builder.dataFetcher("test") { env -> "Success" }
        }
        .build()

    val schemaGenerator = SchemaGenerator()
    return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
  }
}
