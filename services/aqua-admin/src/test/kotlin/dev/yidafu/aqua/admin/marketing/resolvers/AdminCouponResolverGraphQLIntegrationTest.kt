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
import dev.yidafu.aqua.admin.ops.resolvers.WithMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
import java.time.format.DateTimeFormatter

/**
 * 优惠券模板 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-003,AC-MKT-003-01 + 发放闭环)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminCouponResolver GraphQL Integration Tests")
class AdminCouponResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var couponTemplateRepository: CouponTemplateRepository

  @Autowired
  private lateinit var userCouponRepository: UserCouponRepository

  @Autowired
  private lateinit var customerGroupRepository: CustomerGroupRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    // 幂等种子 2 用户 + 1 分组(按 openid/名称去重,避免共享 H2 遗留数据重复)
    if (userRepository.findByWechatOpenId("mkt-coupon-seed-a") == null) {
      userRepository.save(
        UserModel(
          wechatOpenId = "mkt-coupon-seed-a",
          nickname = "券用户A",
          email = "",
          status = UserStatus.ACTIVE,
          role = UserRole.USER,
        ),
      )
    }
    if (userRepository.findByWechatOpenId("mkt-coupon-seed-b") == null) {
      userRepository.save(
        UserModel(
          wechatOpenId = "mkt-coupon-seed-b",
          nickname = "券用户B",
          email = "",
          status = UserStatus.ACTIVE,
          role = UserRole.USER,
        ),
      )
    }
    if (customerGroupRepository.findByName("券发放组") == null) {
      val group =
        customerGroupRepository.save(
          CustomerGroupModel(name = "券发放组", description = "定向发放分组(种子)"),
        )
      userRepository.findByWechatOpenId("mkt-coupon-seed-b")!!.also {
        it.customerGroupId = group.id
        userRepository.save(it)
      }
    }
  }

  private fun postGraphql(query: String): String {
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

  /**
   * 经 GraphQL 新建一张现金券(满 50 减 10,全场,固定区间未来,自动领取),返回模板 id。
   */
  private fun createCashTemplate(
    name: String,
    perUserLimit: Int = 1,
  ): Long {
    val validFrom = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val validTo = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val json =
      postGraphql(
        """mutation { createCouponTemplate(input: {name: "$name", couponType: CASH, thresholdCents: 5000, discountAmountCents: 1000, scope: ALL, validityType: FIXED_RANGE, validFrom: "$validFrom", validTo: "$validTo", issuanceType: AUTO_CLAIM, perUserLimit: $perUserLimit}) { id name } }""",
      )
    assertTrue(!json.contains("\"errors\""), "createCouponTemplate should succeed: $json")
    return couponTemplateRepository.findByName(name)!!.id!!
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("createCouponTemplate 新建现金券并上架(AC-MKT-003-01)")
  fun `createCouponTemplate creates active cash coupon`() {
    val name = "现金券${System.currentTimeMillis()}"
    val validFrom = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val validTo = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val json =
      postGraphql(
        """mutation { createCouponTemplate(input: {name: "$name", couponType: CASH, thresholdCents: 5000, discountAmountCents: 1000, scope: ALL, validityType: FIXED_RANGE, validFrom: "$validFrom", validTo: "$validTo", issuanceType: AUTO_CLAIM, perUserLimit: 1}) { id name couponType thresholdCents discountAmountCents scope validityType issuanceType perUserLimit status } }""",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"couponType\" : \"CASH\"") &&
        json.contains("\"status\" : \"ACTIVE\"") &&
        !json.contains("\"errors\""),
    )
    val template = couponTemplateRepository.findByName(name)!!
    assertTrue(template.couponType == CouponType.CASH)
    assertTrue(template.thresholdCents == 5000L && template.discountAmountCents == 1000L)
    assertTrue(template.scope == CouponScope.ALL && template.status == CouponTemplateStatus.ACTIVE)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_READ"])
  @DisplayName("couponTemplates 列表返回 receivedCount/usedCount 初始为 0")
  fun `couponTemplates query returns zero counts`() {
    // 查询方法仅带 MKT_READ,建券直接种子到仓库(避免权限交叉)
    val name = "查询券${System.currentTimeMillis()}"
    couponTemplateRepository.save(
      CouponTemplateModel(
        name = name,
        couponType = CouponType.CASH,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        scope = CouponScope.ALL,
        validityType = ValidityType.FIXED_RANGE,
        validFrom = LocalDateTime.now().plusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
        issuanceType = CouponIssuanceType.AUTO_CLAIM,
        perUserLimit = 1,
        status = CouponTemplateStatus.ACTIVE,
      ),
    )
    val json =
      postGraphql(
        "{ couponTemplates(input: {keyword: \"$name\"}) { content { name receivedCount usedCount status } } }",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"receivedCount\" : 0") &&
        json.contains("\"usedCount\" : 0") &&
        !json.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("issueCoupons 按用户发放 1 张并落快照")
  fun `issueCoupons to user issues coupon with snapshot`() {
    val name = "发放券${System.currentTimeMillis()}"
    val templateId = createCashTemplate(name)
    val userA = userRepository.findByWechatOpenId("mkt-coupon-seed-a")!!

    val json =
      postGraphql(
        """mutation { issueCoupons(input: {couponTemplateId: $templateId, targetType: USER, userIds: [${userA.id}]}) }""",
      )
    assertTrue(json.contains("\"issueCoupons\" : 1") && !json.contains("\"errors\""))

    // 实例快照:状态 RECEIVED,名称/门槛/面额拷贝自模板
    val coupon = userCouponRepository.findByUserId(userA.id!!).first { it.couponTemplateId == templateId }
    assertTrue(coupon.status == UserCouponStatus.RECEIVED)
    assertTrue(coupon.name == name && coupon.thresholdCents == 5000L && coupon.discountAmountCents == 1000L)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("issueCoupons 按分组发放全体成员")
  fun `issueCoupons to group issues to all members`() {
    val name = "分组券${System.currentTimeMillis()}"
    val templateId = createCashTemplate(name)
    val groupId = customerGroupRepository.findByName("券发放组")!!.id!!
    val userB = userRepository.findByWechatOpenId("mkt-coupon-seed-b")!!

    val json =
      postGraphql(
        """mutation { issueCoupons(input: {couponTemplateId: $templateId, targetType: GROUP, customerGroupId: $groupId}) }""",
      )
    assertTrue(json.contains("\"issueCoupons\" : 1") && !json.contains("\"errors\""))
    assertTrue(userCouponRepository.findByUserId(userB.id!!).any { it.couponTemplateId == templateId })
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("issueCoupons 已达限领数的用户被跳过")
  fun `issueCoupons skips users at perUserLimit`() {
    val name = "限领券${System.currentTimeMillis()}"
    val templateId = createCashTemplate(name, perUserLimit = 1)
    val userA = userRepository.findByWechatOpenId("mkt-coupon-seed-a")!!
    // 已持 1 张(模板限领 1)
    userCouponRepository.save(
      UserCouponModel(
        userId = userA.id!!,
        couponTemplateId = templateId,
        status = UserCouponStatus.RECEIVED,
        name = name,
        couponType = CouponType.CASH,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        scope = CouponScope.ALL,
        expireTime = LocalDateTime.now().plusDays(30),
        claimTime = LocalDateTime.now(),
      ),
    )

    val json =
      postGraphql(
        """mutation { issueCoupons(input: {couponTemplateId: $templateId, targetType: USER, userIds: [${userA.id}]}) }""",
      )
    assertTrue(json.contains("\"issueCoupons\" : 0") && !json.contains("\"errors\""))
    assertTrue(userCouponRepository.countByUserIdAndCouponTemplateId(userA.id!!, templateId) == 1L)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("disableCouponTemplate 停用模板状态置 DISABLED")
  fun `disableCouponTemplate sets status disabled`() {
    val name = "停用券${System.currentTimeMillis()}"
    val templateId = createCashTemplate(name)

    val json = postGraphql("mutation { disableCouponTemplate(id: $templateId) { id name status } }")
    assertTrue(json.contains("\"status\" : \"DISABLED\"") && !json.contains("\"errors\""))
    assertTrue(couponTemplateRepository.findById(templateId).get().status == CouponTemplateStatus.DISABLED)
  }
}
