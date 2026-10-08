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
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelFeeRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseItemRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
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
import java.time.LocalDateTime

/**
 * 水桶费管理 Resolver GraphQL 集成测试(PRD 14-misc.md §G06)
 * 权限:查询 PERMISSION_PAYMENT_READ,变更 PERMISSION_PAYMENT_WRITE/REFUND。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/barrel/"])
@ApplyExtension(SpringExtension::class)
class AdminBarrelResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  @Autowired private val adminRepository: AdminRepository,
  @Autowired private val barrelFeeRepository: BarrelFeeRepository,
  @Autowired private val barrelPurchaseRepository: BarrelPurchaseRepository,
  @Autowired private val barrelPurchaseItemRepository: BarrelPurchaseItemRepository,
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

    fun seedBarrelFee(specification: String): Long =
      barrelFeeRepository
        .save(
          BarrelFeeModel(
            specification = specification,
            priceCents = 4000,
            status = BarrelFeeStatus.ENABLED,
            sortOrder = 1,
          ),
        ).id!!

    fun seedPaidPurchase(feeId: Long): Long {
      val purchase =
        barrelPurchaseRepository.save(
          BarrelPurchaseModel(
            userId = 100L,
            orderNo = "BK${System.nanoTime()}",
            totalAmountCents = 4000,
            paymentStatus = BarrelPaymentStatus.PAID,
            paidAt = LocalDateTime.now(),
          ),
        )
      barrelPurchaseItemRepository.save(
        BarrelPurchaseItemModel(
          purchaseId = purchase.id!!,
          barrelFeeId = feeId,
          specification = "19L/桶",
          unitPriceCents = 4000,
          quantity = 1,
          amountCents = 4000,
        ),
      )
      return purchase.id!!
    }

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子管理员(按用户名去重,退款/价目变更解析器需校验管理员存在)
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
    }

    test("createBarrelFee creates enabled fee") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_WRITE", "PERMISSION_PAYMENT_READ"))
      val spec = "19L/桶${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createBarrelFee(input: {specification: "$spec", priceCents: 4000, sortOrder: 1}) { id specification priceCents status sortOrder } }""",
        )
      withClue("createBarrelFee should succeed: $json") {
        json.contains("\"$spec\"") &&
          json.contains("\"priceCents\" : \"4000\"") &&
          json.contains("\"status\" : \"ENABLED\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
      val fee = barrelFeeRepository.findBySpecification(spec)
      (fee != null && fee!!.priceCents == 4000L && fee.status == BarrelFeeStatus.ENABLED) shouldBe true
    }

    test("createBarrelFee rejects duplicate specification") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_WRITE", "PERMISSION_PAYMENT_READ"))
      val spec = "18.9L/桶${System.currentTimeMillis()}"
      seedBarrelFee(spec)
      val json =
        postGraphql(
          """mutation { createBarrelFee(input: {specification: "$spec", priceCents: 5000, sortOrder: 2}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }

    test("disableBarrelFee marks DISABLED") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_WRITE", "PERMISSION_PAYMENT_READ"))
      val id = seedBarrelFee("11.3L/桶${System.currentTimeMillis()}")
      val json =
        postGraphql(
          """mutation { disableBarrelFee(id: $id) { id status } }""",
        )
      withClue("disableBarrelFee should succeed: $json") {
        json.contains("\"status\" : \"DISABLED\"") && !json.contains("\"errors\"") shouldBe true
      }
      barrelFeeRepository.findById(id).get().status shouldBe BarrelFeeStatus.DISABLED
    }

    test("barrelFees lists seeded fees") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_READ"))
      val spec = "19L-列表${System.currentTimeMillis()}"
      seedBarrelFee(spec)
      val json =
        postGraphql(
          """{ barrelFees(input: {status: ENABLED, page: 0, size: 20}) { content { id specification } totalElements } }""",
        )
      withClue("barrelFees should list seeded fee: $json") {
        json.contains("\"$spec\"") && !json.contains("\"errors\"") shouldBe true
      }
    }

    test("barrelPurchases lists seeded purchase with items") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_READ"))
      val feeId = seedBarrelFee("19L/桶记录${System.currentTimeMillis()}")
      val purchaseId = seedPaidPurchase(feeId)
      val json =
        postGraphql(
          "{ barrelPurchases(input: {status: PAID, page: 0, size: 20}) { content { id orderNo totalAmountCents paymentStatus items { specification quantity } } totalElements } }",
        )
      withClue("barrelPurchases should list seeded purchase: $json") {
        json.contains("\"$purchaseId\"") &&
          json.contains("\"paymentStatus\" : \"PAID\"") &&
          json.contains("\"19L/桶\"") &&
          json.contains("\"quantity\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("refundBarrelPurchase marks REFUNDED (no transaction id)") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_REFUND", "PERMISSION_PAYMENT_READ"))
      val feeId = seedBarrelFee("19L/桶退款${System.currentTimeMillis()}")
      val purchaseId = seedPaidPurchase(feeId)
      val json =
        postGraphql(
          """mutation { refundBarrelPurchase(purchaseId: $purchaseId, remark: "空桶已退回") { id paymentStatus refundRemark } }""",
        )
      withClue("refundBarrelPurchase should mark refunded: $json") {
        json.contains("\"paymentStatus\" : \"REFUNDED\"") &&
          json.contains("\"refundRemark\" : \"空桶已退回\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
      barrelPurchaseRepository.findById(purchaseId).get().paymentStatus shouldBe BarrelPaymentStatus.REFUNDED
    }

    test("refundBarrelPurchase rejects PENDING purchase") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_REFUND", "PERMISSION_PAYMENT_READ"))
      val purchase =
        barrelPurchaseRepository.save(
          BarrelPurchaseModel(
            userId = 100L,
            orderNo = "BK${System.nanoTime()}",
            totalAmountCents = 4000,
            paymentStatus = BarrelPaymentStatus.PENDING,
          ),
        )
      val json =
        postGraphql(
          """mutation { refundBarrelPurchase(purchaseId: ${purchase.id}, remark: null) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
  })
