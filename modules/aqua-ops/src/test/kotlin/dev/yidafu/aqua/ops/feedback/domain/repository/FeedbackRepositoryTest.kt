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

package dev.yidafu.aqua.ops.feedback.domain.repository

import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.ops.common.exposed.OpsPgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 意见反馈仓储实现测试（Testcontainers 真实 PG + Exposed，F-OPS-003）。
 * 重点覆盖：`images` json 列的 ArrayNode 序列化回环（ArrayNodeConverter 路径）、
 * 软删过滤与组合筛选（迁移方案 §9 R3/R4）。
 */
class FeedbackRepositoryTest :
  FunSpec({
    val database = OpsPgTestSupport.database
    val repository = FeedbackRepository(database)

    beforeSpec { OpsPgTestSupport.reset() }

    fun images(vararg urls: String): ArrayNode = jacksonObjectMapper().valueToTree(urls.toList())

    fun feedback(
      userId: Long,
      content: String = "水质发黄",
      contactInfo: String? = "13800000000",
      status: FeedbackStatus = FeedbackStatus.PENDING,
      type: FeedbackType = FeedbackType.COMPLAINT,
      createdAt: LocalDateTime = LocalDateTime.now(),
    ): FeedbackModel =
      FeedbackModel(
        userId = userId,
        feedbackType = type,
        content = content,
        images = images("/static/f1.png", "/static/f2.png"),
        contactInfo = contactInfo,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    test("save 新建：images json 列按字符串回环，内容逐字一致") {
      OpsPgTestSupport.reset()
      val saved = repository.save(feedback(100L))

      val loaded = repository.findById(saved.id!!).orElseThrow()
      loaded.content shouldBe "水质发黄"
      loaded.images.shouldNotBeNull()
      loaded.images!!.toList().map { it.asText() } shouldBe listOf("/static/f1.png", "/static/f2.png")
      loaded.status shouldBe FeedbackStatus.PENDING
    }

    test("save 更新：处理结果与状态覆盖") {
      OpsPgTestSupport.reset()
      val saved = repository.save(feedback(100L))
      saved.status = FeedbackStatus.RESOLVED
      saved.handleResult = "已上门换水"
      saved.handledById = 9L
      saved.handledByName = "管理员"
      saved.handledAt = LocalDateTime.now()
      repository.save(saved)

      val reloaded = repository.findById(saved.id!!).orElseThrow()
      reloaded.status shouldBe FeedbackStatus.RESOLVED
      reloaded.handleResult shouldBe "已上门换水"
      reloaded.handledByName shouldBe "管理员"
    }

    test("delete 后 findById/count/findFirstByContentContaining 全部过滤") {
      OpsPgTestSupport.reset()
      val saved = repository.save(feedback(100L, content = "唯一关键词水垢"))
      val totalBefore = repository.count()
      repository.delete(saved)

      repository.findById(saved.id!!).isPresent shouldBe false
      repository.count() shouldBe totalBefore - 1
      repository.findFirstByContentContaining("水垢") shouldBe null
    }

    test("findFirstByContentContaining 命中首条") {
      OpsPgTestSupport.reset()
      repository.save(feedback(100L, content = "投诉夜间噪音"))

      repository.findFirstByContentContaining("噪音").shouldNotBeNull()
      repository.findFirstByContentContaining("投诉").shouldNotBeNull()
    }

    test("findByUserId 分页仅含该用户") {
      OpsPgTestSupport.reset()
      repository.save(feedback(1L, content = "A用户的反馈"))
      repository.save(feedback(1L, content = "A用户的第二条"))
      repository.save(feedback(2L, content = "B用户的反馈"))

      val page = repository.findByUserId(1L, PageRequest.of(0, 10))
      page.totalElements shouldBe 2
      page.content.all { it.userId == 1L } shouldBe true
    }

    test("findPage：状态/用户/联系方式关键字/时间范围组合过滤") {
      OpsPgTestSupport.reset()
      val now = LocalDateTime.now()
      repository.save(feedback(1L, content = "匹配项", contactInfo = "zhang@aq.cn", createdAt = now))
      repository.save(feedback(2L, content = "其他用户", status = FeedbackStatus.RESOLVED, createdAt = now))
      repository.save(
        feedback(1L, content = "旧时间", contactInfo = "zhang@aq.cn", status = FeedbackStatus.PROCESSING, createdAt = now.minusDays(30)),
      )

      repository
        .findPage(FeedbackFilter(status = FeedbackStatus.PENDING, userId = 1L), PageRequest.of(0, 10))
        .totalElements shouldBe 1
      repository
        .findPage(FeedbackFilter(contactKeyword = "ZHANG"), PageRequest.of(0, 10))
        .totalElements shouldBe 2
      repository
        .findPage(
          FeedbackFilter(dateFrom = now.minusMinutes(1), dateTo = now.plusMinutes(1)),
          PageRequest.of(0, 10),
        ).totalElements shouldBe 2
    }

    test("findPage：dateFrom 单独传入也生效（两端配对由服务层负责）") {
      OpsPgTestSupport.reset()
      val now = LocalDateTime.now()
      repository.save(feedback(1L, content = "仅起始时间", createdAt = now.minusDays(30)))

      repository
        .findPage(FeedbackFilter(dateFrom = now.minusMinutes(1)), PageRequest.of(0, 10))
        .totalElements shouldBe 0
    }
  })
