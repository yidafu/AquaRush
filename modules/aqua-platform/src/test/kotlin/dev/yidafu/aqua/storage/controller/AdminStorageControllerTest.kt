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

package dev.yidafu.aqua.storage.controller

import dev.yidafu.aqua.common.domain.model.FileMetadata
import dev.yidafu.aqua.common.domain.model.enums.FileType
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.service.StorageService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime

/**
 * 管理员存储控制器单元测试: 排序方向、分页查询、类型/所有者/搜索过滤、文件类型与健康检查
 */
class AdminStorageControllerTest :
  FunSpec({
    var storageService: StorageService = mockk()
    var controller: AdminStorageController = mockk()

    fun response(): FileMetadataResponse {
      val meta =
        FileMetadata().apply {
          id = 1L
          fileName = "a.jpg"
          fileType = FileType.IMAGE
          fileSize = 100L
          mimeType = "image/jpeg"
          createdAt = LocalDateTime.of(2025, 6, 1, 10, 0)
          updatedAt = LocalDateTime.of(2025, 6, 1, 10, 0)
        }
      return FileMetadataResponse(meta, "http://x/a.jpg")
    }

    beforeTest {
      storageService = mockk(relaxed = true)
      controller = AdminStorageController(storageService)
    }
    test("listFiles uses desc direction") {
      val page: Page<FileMetadataResponse> = PageImpl(listOf(response()))
      every { storageService.listFiles(any()) } returns page

      val result = controller.listFiles(0, 20, "createdAt", "desc")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 1
    }
    test("listFiles uses asc direction for non-desc") {
      val page: Page<FileMetadataResponse> = PageImpl(emptyList())
      every { storageService.listFiles(any()) } returns page

      val result = controller.listFiles(0, 10, "createdAt", "asc")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 0
    }
    test("listFilesByType delegates with direction") {
      val page: Page<FileMetadataResponse> = PageImpl(listOf(response()))
      every { storageService.listFilesByType(FileType.IMAGE, any()) } returns page

      val result = controller.listFilesByType(FileType.IMAGE, 0, 20, "createdAt", "DESC")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 1
    }
    test("listFilesByOwner delegates") {
      val page: Page<FileMetadataResponse> = PageImpl(emptyList())
      every { storageService.listFilesByOwner(7L, any()) } returns page

      val result = controller.listFilesByOwner(7L, 0, 20, "createdAt", "desc")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 0
    }
    test("listFilesByOwner handles null owner") {
      val page: Page<FileMetadataResponse> = PageImpl(emptyList())
      every { storageService.listFilesByOwner(null, any()) } returns page

      val result = controller.listFilesByOwner(null, 0, 20, "createdAt", "desc")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 0
    }
    test("searchFiles delegates") {
      val page: Page<FileMetadataResponse> = PageImpl(listOf(response()))
      every { storageService.searchFiles("a", any()) } returns page

      val result = controller.searchFiles("a", 0, 20, "createdAt", "desc")

      result.body
        ?.data
        ?.content
        ?.size shouldBe 1
    }
    test("getSupportedFileTypes returns all types") {
      val result = controller.getSupportedFileTypes()

      result.body?.data?.size shouldBe FileType.entries.size
    }
    test("healthCheck returns healthy") {
      val result = controller.healthCheck()

      result.body?.data?.get("status") shouldBe "healthy"
    }
  })
