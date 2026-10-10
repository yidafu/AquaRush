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

package dev.yidafu.aqua.admin.deposit.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRefundApplicationRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 押金管理 Resolver GraphQL 集成测试(B2 押金模型回退,PRD R-MON-03/R-PRO-05):
 * 押金价目 upsert(新增/重复拦截/编辑)+ 押金记录分页 + 退押申请分页。
 * 权限:查询 PERMISSION_PRODUCT_READ,价目变更 PERMISSION_PRODUCT_WRITE(设计02 §5.1)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/barrel/"])
@ApplyExtension(SpringExtension::class)
class AdminDepositResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  @Autowired private val adminRepository: AdminRepository,
  @Autowired private val depositPriceRepository: DepositPriceRepository,
  @Autowired private val depositRecordRepository: DepositRecordRepository,
  @Autowired private val applicationRepository: DepositRefundApplicationRepository,
  @Autowired private val userRepository: UserRepository,
  @Autowired private val addressRepository: AddressRepository,
  @Autowired private val orderRepository: OrderRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun postGraphql(query: String): String {
      val escaped = query.replace("\"", "\\\"")
      val result =
        mockMvc
          .perform(
            post("/graphql")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""{"query": "$escaped"}"""),
          ).andExpect(status().isOk)
          .andReturn()
      return result.response.contentAsString
    }

    fun seedDepositPrice(
      specification: String,
      depositCents: Long = 4000,
    ): Long =
      depositPriceRepository
        .save(
          DepositPriceModel(
            specification = specification,
            priceCents = depositCents,
            status = DepositPriceStatus.ENABLED,
            sortOrder = 1,
          ),
        ).id!!

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子管理员(按用户名去重)
      if (adminRepository.findByUsername("admin") == null) {
        adminRepository.save(
          AdminModel(
            username = "admin",
            passwordHash = "x",
            realName = "管理员",
            role = AdminRoleModel.ADMIN,
          ),
        )
      }

      // deposit_records / deposit_refund_applications.user_id 有外键,固定用户 id 100
      userRepository.save(
        UserModel(
          id = 100L,
          wechatOpenId = "deposit-user-100",
          email = "deposit-user-100@test.local",
        ),
      )
    }

    test("upsertDepositPrice 新增(id 空)落启用价目") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_WRITE", "PERMISSION_PRODUCT_READ"))
      val spec = "19L/桶${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { upsertDepositPrice(input: {specification: "$spec", depositCents: 4000, status: ENABLED, sortOrder: 1}) { id specification depositCents status sortOrder } }""",
        )
      withClue("upsertDepositPrice should succeed: $json") {
        json.contains("\"$spec\"") &&
          json.contains("\"depositCents\" : \"4000\"") &&
          json.contains("\"status\" : \"ENABLED\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
      val price = depositPriceRepository.findBySpecification(spec)
      (price != null && price!!.priceCents == 4000L && price.status == DepositPriceStatus.ENABLED) shouldBe true
    }

    test("upsertDepositPrice 规格重复拦截") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_WRITE", "PERMISSION_PRODUCT_READ"))
      val spec = "18.9L/桶${System.currentTimeMillis()}"
      seedDepositPrice(spec)
      val json =
        postGraphql(
          """mutation { upsertDepositPrice(input: {specification: "$spec", depositCents: 5000, status: ENABLED, sortOrder: 2}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }

    test("upsertDepositPrice 编辑(id 命中)可改状态") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_WRITE", "PERMISSION_PRODUCT_READ"))
      val spec = "11.3L/桶${System.currentTimeMillis()}"
      val id = seedDepositPrice(spec)
      val json =
        postGraphql(
          """mutation { upsertDepositPrice(input: {id: $id, specification: "$spec", depositCents: 4000, status: DISABLED, sortOrder: 3}) { id status } }""",
        )
      withClue("upsertDepositPrice disable should succeed: $json") {
        json.contains("\"status\" : \"DISABLED\"") && !json.contains("\"errors\"") shouldBe true
      }
      depositPriceRepository.findById(id).get().status shouldBe DepositPriceStatus.DISABLED
    }

    test("depositPrices 列出种子价目") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_READ"))
      val spec = "19L-列表${System.currentTimeMillis()}"
      seedDepositPrice(spec)
      val json =
        postGraphql(
          """{ depositPrices(input: {status: ENABLED, page: 0, size: 20}) { content { id specification depositCents } totalElements } }""",
        )
      withClue("depositPrices should list seeded price: $json") {
        json.contains("\"$spec\"") && !json.contains("\"errors\"") shouldBe true
      }
    }

    test("depositRecords 按用户列出押金记录") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_READ"))
      // deposit_records.order_id 有外键,先种地址与完成订单
      val addressId =
        addressRepository
          .save(
            dev.yidafu.aqua.common.domain.model.AddressModel(
              userId = 100L,
              receiverName = "张三",
              phone = "13800000000",
              province = "广东省",
              city = "深圳市",
              district = "南山区",
              detailAddress = "测试路1号",
            ),
          ).id!!
      val orderId =
        orderRepository
          .save(
            dev.yidafu.aqua.common.domain.model.OrderModel(
              orderNo = "SO-DR-${System.nanoTime()}",
              userId = 100L,
              addressId = addressId,
              amountCents = 4000L,
              status = OrderModelStatus.COMPLETED,
            ),
          ).id!!
      depositRecordRepository.saveAll(
        listOf(
          DepositRecordModel(
            orderId = orderId,
            userId = 100L,
            depositAmountCents = 4000,
            specification = "19L/桶",
            unitDepositCents = 4000,
            bucketCount = 1,
            status = DepositStatus.HELD,
          ),
        ),
      )
      val json =
        postGraphql(
          """{ depositRecords(input: {userId: 100, status: HELD, page: 0, size: 20}) { content { specification depositAmountCents status } totalElements } }""",
        )
      withClue("depositRecords should list seeded record: $json") {
        json.contains("\"depositAmountCents\" : \"4000\"") &&
          json.contains("\"status\" : \"HELD\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("depositRefundApplications 列出种子申请") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PRODUCT_READ"))
      val app =
        applicationRepository.save(
          DepositRefundApplicationModel(
            userId = 100L,
            specification = "19L/桶",
            bucketCount = 1,
            depositCents = 4000,
            receiverName = "张三",
            receiverPhone = "13800000000",
            receiverDetail = "某小区1栋101",
            status = DepositRefundApplicationStatus.PENDING,
          ),
        )
      val json =
        postGraphql(
          """{ depositRefundApplications(status: PENDING, page: 0, size: 20) { content { id specification bucketCount depositCents status } totalElements } }""",
        )
      withClue("depositRefundApplications should list seeded application: $json") {
        json.contains("\"${app.id}\"") &&
          json.contains("\"status\" : \"PENDING\"") &&
          json.contains("\"depositCents\" : \"4000\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
  })
