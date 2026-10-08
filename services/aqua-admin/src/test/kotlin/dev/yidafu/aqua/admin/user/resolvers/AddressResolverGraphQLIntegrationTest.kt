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
import dev.yidafu.aqua.admin.testutil.setMockUser
import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.mockk
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * Integration tests for AddressResolver (admin).
 * Mirrors AdminSecurityIntegrationTest's MockMvc pattern（WebEnvironment.MOCK）.
 *
 * /graphql 已开启认证：通过 @WithMockUser 授予 ADDRESS_READ/WRITE 权限绕过 JWT 链路。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/address/"])
@ApplyExtension(SpringExtension::class)
class AddressResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  addressService: AddressService,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun createSampleAddress(
      userId: Long = 1L,
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

    fun graphql(body: String) =
      mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk)

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }
    test("addresses query returns all addresses") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      addressService.save(createSampleAddress())

      graphql("""{"query":"{ addresses { id userId receiverName phone province city district detailAddress isDefault } }"}""")
        .andExpect(jsonPath("$.data.addresses.length()").value(greaterThanOrEqualTo(1)))
    }
    test("searchAllAddresses query searches addresses") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      addressService.save(createSampleAddress(city = "深圳市", detailAddress = "南山科技园"))

      // searchAllAddresses 仅按 detailAddress / phone / receiverName 模糊匹配，不匹配 city
      graphql(
        """{"query":"{ searchAllAddresses(keyword: \"科技园\", page: 0, size: 10) { content { id receiverName phone province city district detailAddress } totalElements totalPages size number } }"}""",
      ).andExpect(jsonPath("$.data.searchAllAddresses.content.length()").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.data.searchAllAddresses.totalElements").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.data.searchAllAddresses.size").value(10))
        .andExpect(jsonPath("$.data.searchAllAddresses.number").value(0))
    }
    test("createAdminAddress mutation creates address") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      graphql(
        """{"query":"mutation createAddress(${'$'}input: AddressInput!) { createAdminAddress(input: ${'$'}input) { id userId receiverName phone province city district detailAddress isDefault } }","variables":{"input":{"receiverName":"测试用户","phone":"13800138000","province":"广东省","provinceCode":"440000","city":"深圳市","cityCode":"440300","district":"南山区","districtCode":"440305","detailAddress":"测试地址1号","isDefault":false}}}""",
      ).andExpect(jsonPath("$.data.createAdminAddress.receiverName").value("测试用户"))
    }
    test("updateAdminAddress mutation updates existing address") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      val savedAddress = addressService.save(createSampleAddress())

      graphql(
        """{"query":"mutation updateAddress(${'$'}id: Long!, ${'$'}input: UpdateAddressInput!) { updateAdminAddress(id: ${'$'}id, input: ${'$'}input) { id receiverName phone province city district detailAddress isDefault } }","variables":{"id":${savedAddress.id},"input":{"receiverName":"李四","phone":"13800138000","province":"广东省","provinceCode":"440000","city":"深圳市","cityCode":"440300","district":"南山区","districtCode":"440305","detailAddress":"更新后的地址","isDefault":false}}}""",
      ).andExpect(jsonPath("$.data.updateAdminAddress.id").value(savedAddress.id))
        .andExpect(jsonPath("$.data.updateAdminAddress.receiverName").value("李四"))
        .andExpect(jsonPath("$.data.updateAdminAddress.detailAddress").value("更新后的地址"))
        .andExpect(jsonPath("$.data.updateAdminAddress.phone").value("13800138000"))
    }
    test("deleteAdminAddress mutation deletes address") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      val savedAddress = addressService.save(createSampleAddress())

      graphql(
        """{"query":"mutation deleteAddress(${'$'}id: Long!) { deleteAdminAddress(id: ${'$'}id) }","variables":{"id":${savedAddress.id}}}""",
      ).andExpect(jsonPath("$.data.deleteAdminAddress").value(true))
    }
    test("userAddresses query returns addresses for specific user") {
      setMockUser(authorities = listOf("PERMISSION_ADDRESS_READ", "PERMISSION_ADDRESS_WRITE"))
      addressService.save(createSampleAddress(userId = 1L))

      graphql("""{"query":"{ userAddresses(userId: 1) { id userId receiverName phone province city district detailAddress } }"}""")
        .andExpect(jsonPath("$.data.userAddresses.length()").value(greaterThanOrEqualTo(1)))
    }
  })
