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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.security.toPermissionAuthorities
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for AddressResolver (admin)
 * Tests the GraphQL endpoint with real Spring context and H2 database.
 *
 * /graphql 已开启认证：@BeforeAll 中种子化一个 ADMIN 角色管理员并为其签发 JWT，
 * 请求附带 Authorization header 通过 URL 与方法级权限校验。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/test-schema.graphqls"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AddressResolver GraphQL Integration Tests")
class AddressResolverGraphQLIntegrationTest {
  @Value("\${local.server.port}")
  private var port: Int = 0

  @Autowired
  private lateinit var addressService: AddressService

  @Autowired
  private lateinit var adminRepository: AdminRepository

  @Autowired
  private lateinit var jwtTokenService: JwtTokenService

  private lateinit var webGraphQlTester: HttpGraphQlTester

  private var testUserId: Long = 1L

  @BeforeAll
  fun setUp() {
    // 种子化 ADMIN 角色管理员并签发 JWT（ADMIN 具备 PERMISSION_ADDRESS_READ/WRITE）
    val admin =
      AdminModel(
        id = 1001L,
        username = "testadmin",
        passwordHash = "unused-in-test",
        role = AdminRoleModel.ADMIN,
      )
    adminRepository.save(admin)
    val token =
      jwtTokenService.generateAccessToken(
        UserPrincipal(
          id = admin.id!!,
          _username = admin.username,
          userType = admin.role.name,
          _authorities = admin.role.toPermissionAuthorities(),
        ),
      )

    val webTestClient =
      WebTestClient
        .bindToServer()
        .baseUrl("http://localhost:$port")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .build()
    webGraphQlTester = HttpGraphQlTester.create(webTestClient)
  }

  private fun createSampleAddress(
    userId: Long = testUserId,
    receiverName: String = "张三",
    phone: String = "13800138000",
    province: String = "广东省",
    city: String = "深圳市",
    district: String = "南山区",
    detailAddress: String = "测试地址1号",
    isDefault: Boolean = true,
  ): AddressModel =
    AddressModel(
      userId = userId,
      receiverName = receiverName,
      phone = phone,
      province = province,
      provinceCode = "440000",
      city = city,
      cityCode = "440300",
      district = district,
      districtCode = "440305",
      detailAddress = detailAddress,
      isDefault = isDefault,
    )

  @Test
  @DisplayName("addresses query should return all addresses for admin")
  fun `addresses query returns all addresses`() {
    // Seed test data
    addressService.save(createSampleAddress())

    // Execute GraphQL query
    val result =
      webGraphQlTester
        .document(
          """
          query {
            addresses {
              id
              userId
              receiverName
              phone
              province
              city
              district
              detailAddress
              isDefault
            }
          }
          """.trimIndent(),
        ).execute()
        .path("data.addresses")
        .entityList(Map::class.java)
        .get()

    org.junit.jupiter.api.Assertions
      .assertTrue(result.size >= 1)
  }

  @Test
  @DisplayName("searchAllAddresses query should search addresses for admin")
  fun `searchAllAddresses query searches addresses`() {
    // Seed test data
    addressService.save(createSampleAddress(city = "深圳市", detailAddress = "南山科技园"))

    // Execute GraphQL query
    val searchResult =
      webGraphQlTester
        .document(
          """
          query searchAddresses(${'$'}keyword: String) {
            searchAllAddresses(keyword: ${'$'}keyword, page: 0, size: 10) {
              id
              receiverName
              phone
              province
              city
              district
              detailAddress
            }
          }
          """.trimIndent(),
        ).variable("keyword", "深圳")
        .execute()
        .path("data.searchAllAddresses")
        .entityList(Map::class.java)
        .get()

    org.junit.jupiter.api.Assertions
      .assertTrue(searchResult.size >= 1)
  }

  @Test
  @DisplayName("createAdminAddress mutation should create address for admin")
  fun `createAdminAddress mutation creates address`() {
    // Execute GraphQL mutation
    webGraphQlTester
      .document(
        """
        mutation createAddress(${'$'}input: AddressInput!) {
          createAdminAddress(input: ${'$'}input) {
            id
            userId
            receiverName
            phone
            province
            city
            district
            detailAddress
            isDefault
          }
        }
        """.trimIndent(),
      ).variable(
        "input",
        mapOf(
          "receiverName" to "测试用户",
          "phone" to "13800138000",
          "province" to "广东省",
          "provinceCode" to "440000",
          "city" to "深圳市",
          "cityCode" to "440300",
          "district" to "南山区",
          "districtCode" to "440305",
          "detailAddress" to "测试地址1号",
          "isDefault" to false,
        ),
      ).execute()
      .path("data.createAdminAddress.receiverName")
      .entity(String::class.java)
      .isEqualTo("测试用户")
  }

  @Test
  @DisplayName("deleteAdminAddress mutation should delete address for admin")
  fun `deleteAdminAddress mutation deletes address`() {
    // Seed test data
    val savedAddress = addressService.save(createSampleAddress())

    // Execute GraphQL mutation
    webGraphQlTester
      .document(
        """
        mutation deleteAddress(${'$'}id: Long!) {
          deleteAdminAddress(id: ${'$'}id)
        }
        """.trimIndent(),
      ).variable("id", savedAddress.id)
      .execute()
      .path("data.deleteAdminAddress")
      .entity(Boolean::class.java)
      .isEqualTo(true)
  }

  @Test
  @DisplayName("userAddresses query should return addresses for specific user")
  fun `userAddresses query returns addresses for specific user`() {
    // Seed test data
    addressService.save(createSampleAddress(userId = testUserId))

    // Execute GraphQL query
    val userAddrResult =
      webGraphQlTester
        .document(
          """
          query userAddresses(${'$'}userId: Long) {
            userAddresses(userId: ${'$'}userId) {
              id
              userId
              receiverName
              phone
              province
              city
              district
              detailAddress
            }
          }
          """.trimIndent(),
        ).variable("userId", testUserId)
        .execute()
        .path("data.userAddresses")
        .entityList(Map::class.java)
        .get()

    org.junit.jupiter.api.Assertions
      .assertTrue(userAddrResult.size >= 1)
  }
}
