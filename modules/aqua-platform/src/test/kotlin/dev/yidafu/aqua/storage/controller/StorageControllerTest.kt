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

import dev.yidafu.aqua.storage.domain.model.FileMetadata
import dev.yidafu.aqua.storage.domain.model.enums.FileType
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.service.StorageService
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 存储控制器单元测试: 上传/下载/图片处理/元数据/删除 的参数透传与响应组装
 */
class StorageControllerTest {
  private lateinit var storageService: StorageService
  private lateinit var controller: StorageController

  @BeforeEach
  fun setUp() {
    storageService = mockk(relaxed = true)
    controller = StorageController(storageService)
  }

  private fun response(): FileMetadataResponse {
    val meta =
      FileMetadata().apply {
        id = 1L
        fileName = "a.jpg"
        fileType = FileType.IMAGE
        fileSize = 100L
        mimeType = "image/jpeg"
        extension = "jpg"
        createdAt = LocalDateTime.of(2025, 6, 1, 10, 0)
        updatedAt = LocalDateTime.of(2025, 6, 1, 10, 0)
        isPublic = true
      }
    return FileMetadataResponse(meta, "http://x/a.jpg")
  }

  private fun file(): MultipartFile =
    object : MultipartFile {
      override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(byteArrayOf(1, 2))

      override fun getName(): String = "file"

      override fun getOriginalFilename(): String? = "a.jpg"

      override fun getContentType(): String? = "image/jpeg"

      override fun isEmpty(): Boolean = false

      override fun getSize(): Long = 2L

      override fun getBytes(): ByteArray = byteArrayOf(1, 2)

      override fun transferTo(dest: java.io.File) {}
    }

  @Test
  fun `uploadFile passes params and returns success`() {
    val resp = response()
    every { storageService.uploadFile(any(), any()) } returns resp

    val result = controller.uploadFile(file(), FileType.IMAGE, "desc", true, 7L)

    assertEquals(resp, result.data)
    assertEquals(true, result.success)
  }

  @Test
  fun `getFile returns resource with filename override`() {
    every { storageService.getFile(1L) } returns ByteArrayResource(byteArrayOf(9))
    every { storageService.getFileMetadata(1L) } returns response()

    val result = controller.getFile(1L, "custom.png")

    assertEquals(200, result.statusCode.value())
    assertEquals("inline; filename=\"custom.png\"", result.headers.getFirst("Content-Disposition"))
    assertEquals("image/jpeg", result.headers.getContentType().toString())
  }

  @Test
  fun `getFile falls back to metadata filename`() {
    every { storageService.getFile(1L) } returns ByteArrayResource(byteArrayOf(9))
    every { storageService.getFileMetadata(1L) } returns response()

    val result = controller.getFile(1L, null)

    assertEquals("inline; filename=\"a.jpg\"", result.headers.getFirst("Content-Disposition"))
  }

  @Test
  fun `getProcessedImage returns jpeg by default`() {
    every { storageService.getProcessedImage(1L, any()) } returns byteArrayOf(1, 2)

    val result = controller.getProcessedImage(1L, 100, null, null, null, false, null, mockk<HttpServletResponse>())

    assertEquals("image/jpeg", result.headers.getContentType().toString())
  }

  @Test
  fun `getProcessedImage maps png format`() {
    every { storageService.getProcessedImage(1L, any()) } returns byteArrayOf(1, 2)

    val result = controller.getProcessedImage(1L, null, null, null, "png", false, null, mockk<HttpServletResponse>())

    assertEquals("image/png", result.headers.getContentType().toString())
  }

  @Test
  fun `getProcessedImage maps webp gif bmp formats`() {
    every { storageService.getProcessedImage(1L, any()) } returns byteArrayOf(1, 2)
    val formats = mapOf("webp" to "image/webp", "gif" to "image/gif", "bmp" to "image/bmp")
    for ((fmt, mime) in formats) {
      val result = controller.getProcessedImage(1L, null, null, null, fmt, false, null, mockk<HttpServletResponse>())
      assertEquals(mime, result.headers.getContentType().toString(), fmt)
    }
  }

  @Test
  fun `getFileMetadata returns success`() {
    val resp = response()
    every { storageService.getFileMetadata(1L) } returns resp

    val result = controller.getFileMetadata(1L)

    assertEquals(200, result.statusCode.value())
    assertEquals(resp, result.body?.data)
  }

  @Test
  fun `deleteFile returns success`() {
    every { storageService.deleteFile(1L) } returns true

    val result = controller.deleteFile(1L)

    assertEquals(true, result.body?.data)
  }
}
