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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
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
 * 用户持有券/票 Resolver GraphQL 集成测试(用户详情页「优惠券/水票」Tab,PRD 07-user.md §F-USR-022)
 * 权限:PERMISSION_USER_READ(与用户详情页一致)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@ApplyExtension(SpringExtension::class)
class AdminUserCouponTicketResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  userRepository: UserRepository,
  userCouponRepository: UserCouponRepository,
  userTicketRepository: UserTicketRepository,
  ticketTypeRepository: TicketTypeRepository,
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

    fun userA(): UserModel = userRepository.findByWechatOpenId("mkt-user-coupon-a")!!

    fun userB(): UserModel = userRepository.findByWechatOpenId("mkt-user-coupon-b")!!

    fun seedCoupon(
      user: UserModel,
      status: UserCouponStatus = UserCouponStatus.RECEIVED,
      name: String = "种子券${System.currentTimeMillis()}",
    ): Long =
      userCouponRepository
        .save(
          UserCouponModel(
            userId = user.id!!,
            couponTemplateId = System.currentTimeMillis(),
            status = status,
            name = name,
            couponType = CouponType.CASH,
            thresholdCents = 5000,
            discountAmountCents = 1000,
            scope = CouponScope.ALL,
            expireTime = LocalDateTime.now().plusDays(30),
            claimTime = LocalDateTime.now(),
          ),
        ).id!!

    fun seedTicket(
      user: UserModel,
      status: UserTicketStatus = UserTicketStatus.UNUSED,
    ): Long {
      val ticketType =
        ticketTypeRepository.save(
          TicketTypeModel(
            name = "种子票种${System.currentTimeMillis()}",
            ticketType = TicketTypeKind.UNIVERSAL,
            specification = "19L",
            packageCount = 10,
            packagePriceCents = 10000,
            giftCount = 1,
            validityType = ValidityType.FIXED_RANGE,
            perUserLimit = 10,
            status = TicketTypeStatus.ACTIVE,
          ),
        )
      return userTicketRepository
        .save(
          UserTicketModel(
            userId = user.id!!,
            ticketTypeId = ticketType.id!!,
            purchaseId = 0L,
            status = status,
            expireTime = LocalDateTime.now().plusDays(90),
            isGift = false,
          ),
        ).id!!
    }

    // 直种子一张券(用户 A,现金券满 50 减 10,全场)
    // 直种子一张票(用户 A,通兑票,限定规格 19L)

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子 2 用户(按 openid 去重,避免共享 H2 遗留数据重复)
      if (userRepository.findByWechatOpenId("mkt-user-coupon-a") == null) {
        userRepository.save(
          UserModel(
            wechatOpenId = "mkt-user-coupon-a",
            nickname = "券票用户A",
            email = "",
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
          ),
        )
      }
      if (userRepository.findByWechatOpenId("mkt-user-coupon-b") == null) {
        userRepository.save(
          UserModel(
            wechatOpenId = "mkt-user-coupon-b",
            nickname = "券票用户B",
            email = "",
            status = UserStatus.ACTIVE,
            role = UserRole.USER,
          ),
        )
      }
    }
    test("userCoupons returns coupons for the user") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      val couponId = seedCoupon(userA())
      val json =
        postGraphql(
          "{ userCoupons(userId: ${userA().id}) { id name couponType thresholdCents discountAmountCents status expireTime usable } }",
        )
      withClue("userCoupons should return seeded coupon: $json") {
        json.contains("\"id\" : \"$couponId\"") &&
          json.contains("\"couponType\" : \"CASH\"") &&
          json.contains("\"thresholdCents\" : \"5000\"") &&
          json.contains("\"discountAmountCents\" : \"1000\"") &&
          json.contains("\"status\" : \"RECEIVED\"") &&
          json.contains("\"usable\" : false") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("userCoupons filters by status") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      seedCoupon(userA(), status = UserCouponStatus.RECEIVED)
      seedCoupon(userA(), status = UserCouponStatus.USED)
      val json = postGraphql("{ userCoupons(userId: ${userA().id}, status: USED) { id status } }")
      withClue("userCoupons should only return USED coupons: $json") {
        json.contains("\"status\" : \"USED\"") &&
          !json.contains("\"RECEIVED\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("userCoupons is isolated per user") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      seedCoupon(userA())
      val json = postGraphql("{ userCoupons(userId: ${userB().id}) { id } }")
      withClue(
        "userB should have no coupons: $json",
      ) { json.contains("\"userCoupons\" : [ ]") && !json.contains("\"errors\"") shouldBe true }
    }
    test("userTickets returns tickets with ticket type info") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      seedTicket(userA())
      val json = postGraphql("{ userTickets(userId: ${userA().id}) { id ticketTypeName specification status expireTime isGift } }")
      withClue("userTickets should return seeded ticket with type info: $json") {
        json.contains("\"ticketTypeName\" : \"种子票种") &&
          json.contains("\"specification\" : \"19L\"") &&
          json.contains("\"status\" : \"UNUSED\"") &&
          json.contains("\"isGift\" : false") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("userTickets filters by status") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      seedTicket(userA(), status = UserTicketStatus.UNUSED)
      seedTicket(userA(), status = UserTicketStatus.USED)
      val json = postGraphql("{ userTickets(userId: ${userA().id}, status: USED) { id status } }")
      withClue("userTickets should only return USED tickets: $json") {
        json.contains("\"status\" : \"USED\"") &&
          !json.contains("\"UNUSED\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("userTickets is isolated per user") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      seedTicket(userA())
      val json = postGraphql("{ userTickets(userId: ${userB().id}) { id } }")
      withClue(
        "userB should have no tickets: $json",
      ) { json.contains("\"userTickets\" : [ ]") && !json.contains("\"errors\"") shouldBe true }
    }
    test("userCoupons anonymous is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ userCoupons(userId: ${userA().id}) { id } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
