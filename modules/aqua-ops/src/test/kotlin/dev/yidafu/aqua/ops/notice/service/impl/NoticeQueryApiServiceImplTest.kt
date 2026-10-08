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

package dev.yidafu.aqua.ops.notice.service.impl

import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 公告查询服务(用户端)单元测试:委托仓库按当前时间过滤已发布且在时间窗内的公告
 */
class NoticeQueryApiServiceImplTest :
  FunSpec({
    var noticeRepository: NoticeRepository = mockk()
    var service: NoticeQueryApiServiceImpl = mockk()

    beforeTest {
      noticeRepository = mockk(relaxed = true)
      service = NoticeQueryApiServiceImpl(noticeRepository)
    }
    test("getActiveNotices delegates to repository") {
      val active = listOf(NoticeModel(id = 1L, title = "生效公告", content = "内容"))
      every { noticeRepository.findActiveNotices(any()) } returns active

      val result = service.getActiveNotices()

      result.size shouldBe 1
      verify { noticeRepository.findActiveNotices(any()) }
    }
  })
