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

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.service.StorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageControllerTest {
  private lateinit var storageService: StorageService
  private lateinit var controller: StorageController

  @BeforeEach
  fun setUp() {
    storageService = mockk()
    controller = StorageController(storageService)
  }

  private fun createFileMetadata(
    id: Long = 1L,
    fileName: String = "test.jpg",
    fileType: FileType = FileType.IMAGE,
    fileSize: Long = 1024L,
    mimeType: String = "image/jpeg",
    extension: String = "jpg",
    isPublic: Boolean = true,
    description: String? = null,
    ownerId: Long? = null,
  ): FileMetadata {
    return FileMetadata(
      fileName = fileName,
      storagePath = "storage/$id/$fileName",
      fileType = fileType,
      fileSize = fileSize,
      mimeType = mimeType,
      checksum = "test_checksum_$id",
      isPublic = isPublic,
      description = description,
      extension = extension,
      ownerId = ownerId,
    ).apply { this.id = id }
  }

  @Test
  fun `uploadFile success`() {
    // Given
    val file =
      MockMultipartFile(
        "file",
        "test.jpg",
        "image/jpeg",
        "test image content".toByteArray(),
      )
    val metadata = createFileMetadata(id = 1L, fileName = "test.jpg")
    val response = FileMetadataResponse(metadata, "/api/v1/storage/files/1")

    every { storageService.uploadFile(any(), any()) } returns response

    // When
    val result = controller.uploadFile(file, FileType.IMAGE, "Test image", true, 1L)

    // Then
    assertNotNull(result)
    assertTrue(result.success)
    assertEquals("test.jpg", result.data?.fileName)
  }

  @Test
  fun `uploadFile with all params`() {
    // Given
    val file =
      MockMultipartFile(
        "file",
        "test.png",
        "image/png",
        "test image content".toByteArray(),
      )
    val metadata = createFileMetadata(id = 2L, fileName = "test.png", ownerId = 100L)
    val response = FileMetadataResponse(metadata, "/api/v1/storage/files/2")

    val requestSlot = slot<FileUploadRequest>()
    every { storageService.uploadFile(any(), capture(requestSlot)) } returns response

    // When
    val result = controller.uploadFile(file, FileType.IMAGE, "Custom description", false, 100L)

    // Then
    assertNotNull(result)
    assertTrue(result.success)
    val request = requestSlot.captured
    assertEquals("Custom description", request.description)
    assertEquals(false, request.isPublic)
    assertEquals(100L, request.ownerId)
  }

  @Test
  fun `getFile success`() {
    // Given
    val fileId = 1L
    val mockResource = mockk<Resource>()
    val metadata = createFileMetadata(id = fileId, fileName = "test.jpg", mimeType = "image/jpeg")
    val responseMetadata = FileMetadataResponse(metadata, "/api/v1/storage/files/1")

    every { storageService.getFile(fileId) } returns mockResource
    every { storageService.getFileMetadata(fileId) } returns responseMetadata

    // When
    val result = controller.getFile(fileId, null)

    // Then
    assertNotNull(result)
    assertEquals(MediaType.IMAGE_JPEG, result.headers.contentType)
    val cacheControl: String? = result.headers.getFirst(HttpHeaders.CACHE_CONTROL)
    assertEquals("max-age=31536000", cacheControl)
  }

  @Test
  fun `getFile with custom name`() {
    // Given
    val fileId = 1L
    val customName = "custom-name.jpg"
    val mockResource = mockk<Resource>()
    val metadata = createFileMetadata(id = fileId, fileName = "original.jpg")
    val responseMetadata = FileMetadataResponse(metadata, "/api/v1/storage/files/1")

    every { storageService.getFile(fileId) } returns mockResource
    every { storageService.getFileMetadata(fileId) } returns responseMetadata

    // When
    val result = controller.getFile(fileId, customName)

    // Then
    assertNotNull(result)
    val contentDisposition = result.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
    assertTrue(contentDisposition?.contains(customName) == true)
  }

  @Test
  fun `getProcessedImage success`() {
    // Given
    val fileId = 1L
    val processedImage = "processed image bytes".toByteArray()
    val parametersSlot = slot<ImageParameters>()

    every { storageService.getProcessedImage(any(), capture(parametersSlot)) } returns processedImage

    // When
    val result = controller.getProcessedImage(
      id = fileId,
      width = 800,
      height = 600,
      quality = 0.8f,
      format = "PNG",
      watermark = false,
      watermarkText = null,
      response = mockk(relaxed = true),
    )

    // Then
    assertNotNull(result)
    assertEquals(MediaType.IMAGE_PNG, result.headers.contentType)
    val cacheControl: String? = result.headers.getFirst(HttpHeaders.CACHE_CONTROL)
    assertEquals("max-age=86400", cacheControl)
    val params = parametersSlot.captured
    assertEquals(800, params.width)
    assertEquals(600, params.height)
    assertEquals(0.8f, params.quality)
    assertEquals("PNG", params.format)
  }

  @Test
  fun `getProcessedImage with watermark`() {
    // Given
    val fileId = 1L
    val processedImage = "watermarked image bytes".toByteArray()
    val parametersSlot = slot<ImageParameters>()

    every { storageService.getProcessedImage(any(), capture(parametersSlot)) } returns processedImage

    // When
    val result = controller.getProcessedImage(
      id = fileId,
      width = null,
      height = null,
      quality = null,
      format = null,
      watermark = true,
      watermarkText = "AquaRush",
      response = mockk(relaxed = true),
    )

    // Then
    assertNotNull(result)
    val params = parametersSlot.captured
    assertTrue(params.watermark)
    assertEquals("AquaRush", params.watermarkText)
  }

  @Test
  fun `getFileMetadata success`() {
    // Given
    val fileId = 1L
    val metadata = createFileMetadata(id = fileId, fileName = "test.jpg")
    val response = FileMetadataResponse(metadata, "/api/v1/storage/files/1")

    every { storageService.getFileMetadata(fileId) } returns response

    // When
    val result = controller.getFileMetadata(fileId)

    // Then
    assertNotNull(result)
    assertNotNull(result.body)
    assertTrue(result.body?.success == true)
    assertEquals(fileId, result.body?.data?.id)
    assertEquals("test.jpg", result.body?.data?.fileName)
  }

  @Test
  fun `deleteFile success`() {
    // Given
    val fileId = 1L
    every { storageService.deleteFile(fileId) } returns true

    // When
    val result = controller.deleteFile(fileId)

    // Then
    assertNotNull(result)
    assertNotNull(result.body)
    assertTrue(result.body?.success == true)
    assertEquals(true, result.body?.data)
  }

  @Test
  fun `deleteFile not found`() {
    // Given
    val fileId = 999L
    every { storageService.deleteFile(fileId) } returns false

    // When
    val result = controller.deleteFile(fileId)

    // Then
    assertNotNull(result)
    assertNotNull(result.body)
    assertTrue(result.body?.success == true)
    assertEquals(false, result.body?.data)
  }
}
