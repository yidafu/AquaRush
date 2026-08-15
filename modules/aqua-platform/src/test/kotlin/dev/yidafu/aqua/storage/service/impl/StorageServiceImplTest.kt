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

package dev.yidafu.aqua.storage.service.impl

import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.model.FileMetadata
import dev.yidafu.aqua.storage.domain.model.enums.FileType
import dev.yidafu.aqua.storage.domain.repository.FileMetadataRepository
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.service.ImageProcessingService
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.tika.Tika
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 存储服务单元测试: 上传校验(空/大小/扩展名/去重)、MIME 检测、读写删、图片处理路由
 */
class StorageServiceImplTest {
  private lateinit var fileMetadataRepository: FileMetadataRepository
  private lateinit var storageStrategy: StorageStrategy
  private lateinit var imageProcessingService: ImageProcessingService
  private lateinit var storageProperties: StorageProperties
  private lateinit var tika: Tika
  private lateinit var service: StorageServiceImpl

  @BeforeEach
  fun setUp() {
    fileMetadataRepository = mockk(relaxed = true)
    storageStrategy = mockk(relaxed = true)
    imageProcessingService = mockk(relaxed = true)
    storageProperties =
      StorageProperties().apply {
        local.maxFileSize = 1024L
        local.allowedExtensions = setOf("jpg", "png", "txt")
        local.baseUrl = "http://localhost:9090"
      }
    tika = mockk(relaxed = true)
    service = StorageServiceImpl(fileMetadataRepository, storageStrategy, imageProcessingService, storageProperties, tika)
  }

  private fun fakeFile(
    name: String = "photo.jpg",
    bytes: ByteArray = byteArrayOf(1, 2, 3),
    contentType: String = "image/jpeg",
  ): MultipartFile =
    object : MultipartFile {
      override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(bytes)

      override fun getName(): String = "file"

      override fun getOriginalFilename(): String? = name

      override fun getContentType(): String? = contentType

      override fun isEmpty(): Boolean = bytes.isEmpty()

      override fun getSize(): Long = bytes.size.toLong()

      override fun getBytes(): ByteArray = bytes

      override fun transferTo(dest: java.io.File) {
        dest.writeBytes(bytes)
      }
    }

  private fun metadata(
    id: Long = 1L,
    fileName: String = "photo.jpg",
    fileType: FileType = FileType.IMAGE,
    extension: String? = "jpg",
  ): FileMetadata =
    FileMetadata().apply {
      this.id = id
      this.fileName = fileName
      this.storagePath = "image/2025/06/$fileName"
      this.fileType = fileType
      this.fileSize = 100L
      this.mimeType = "image/jpeg"
      this.checksum = "abc123"
      this.extension = extension
      this.createdAt = LocalDateTime.of(2025, 6, 1, 10, 0)
    }

  @Test
  fun `uploadFile rejects empty file`() {
    val file = fakeFile(bytes = byteArrayOf())

    assertFailsWith<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
  }

  @Test
  fun `uploadFile rejects oversized file`() {
    val file = fakeFile(bytes = ByteArray(2048))

    assertFailsWith<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
  }

  @Test
  fun `uploadFile rejects disallowed extension`() {
    val file = fakeFile(name = "virus.exe")

    assertFailsWith<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
  }

  @Test
  fun `uploadFile returns existing file by checksum without storing again`() {
    val existing = metadata()
    every { fileMetadataRepository.findByChecksum(any()) } returns Optional.of(existing)
    every { storageStrategy.generateUrl(existing.id!!, existing.fileName) } returns
      "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"

    val response = service.uploadFile(fakeFile(), FileUploadRequest())

    assertEquals(existing.id, response.id)
    assertEquals("http://localhost:9090/api/v1/storage/files/1?name=photo.jpg", response.fileUrl)
    verify(exactly = 0) { storageStrategy.store(any(), any()) }
  }

  @Test
  fun `uploadFile stores new file and saves metadata`() {
    every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
    every { tika.detect(any<java.io.InputStream>(), any<String>()) } returns "image/png"
    every { storageStrategy.store(any(), any()) } returns "image/2025/06/photo.png"
    every { storageStrategy.generateUrl(any(), any()) } returns "http://localhost:9090/api/v1/storage/files/1?name=photo.png"
    every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

    val response = service.uploadFile(fakeFile(name = "photo.png", contentType = "image/png"), FileUploadRequest())

    assertEquals("photo.png", response.fileName)
    assertEquals(FileType.IMAGE, response.fileType)
    verify { storageStrategy.store(any(), any()) }
    verify { fileMetadataRepository.save(any()) }
  }

  @Test
  fun `uploadFile detects file type from mime`() {
    every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
    every { tika.detect(any<java.io.InputStream>(), any<String>()) } returns "application/pdf"
    every { storageStrategy.store(any(), any()) } returns "document/2025/06/doc.pdf"
    every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

    val response = service.uploadFile(fakeFile(name = "doc.txt"), FileUploadRequest())

    assertEquals(FileType.DOCUMENT, response.fileType)
  }

  @Test
  fun `uploadFile honors requested file type`() {
    every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
    every { storageStrategy.store(any(), any()) } returns "archive/2025/06/data.zip"
    every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

    val response = service.uploadFile(fakeFile(name = "data.txt"), FileUploadRequest(fileType = FileType.ARCHIVE))

    assertEquals(FileType.ARCHIVE, response.fileType)
  }

  @Test
  fun `uploadFile wraps io exception in runtime exception`() {
    every { fileMetadataRepository.findByChecksum(any()) } throws IOException("disk error")

    assertFailsWith<RuntimeException> { service.uploadFile(fakeFile(), FileUploadRequest()) }
  }

  @Test
  fun `getFile returns resource for existing metadata`() {
    val meta = metadata()
    val resource = ByteArrayResource(byteArrayOf(9))
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
    every { storageStrategy.retrieve(meta.storagePath) } returns resource

    assertSame(resource, service.getFile(1L))
  }

  @Test
  fun `getFile throws when metadata missing`() {
    every { fileMetadataRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<NoSuchElementException> { service.getFile(1L) }
  }

  @Test
  fun `getProcessedImage returns processed bytes for image`() {
    val meta = metadata()
    val params = ImageParameters(width = 100)
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
    every { imageProcessingService.getOrProcessImage(meta.storagePath, params) } returns byteArrayOf(1, 2)

    val bytes = service.getProcessedImage(1L, params)

    assertEquals(2, bytes.size)
  }

  @Test
  fun `getProcessedImage rejects non-image file`() {
    val meta = metadata(fileType = FileType.DOCUMENT, extension = "pdf")
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)

    assertFailsWith<IllegalArgumentException> { service.getProcessedImage(1L, ImageParameters(width = 100)) }
  }

  @Test
  fun `getFileMetadata returns response with url`() {
    val meta = metadata()
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
    every { storageStrategy.generateUrl(1L, meta.fileName) } returns "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"

    val response = service.getFileMetadata(1L)

    assertEquals(1L, response.id)
    assertEquals("http://localhost:9090/api/v1/storage/files/1?name=photo.jpg", response.fileUrl)
  }

  @Test
  fun `deleteFile deletes storage and metadata`() {
    val meta = metadata()
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
    every { storageStrategy.delete(meta.storagePath) } returns true

    assertTrue(service.deleteFile(1L))
    verify { fileMetadataRepository.delete(meta) }
  }

  @Test
  fun `deleteFile returns false when storage delete fails`() {
    val meta = metadata()
    every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
    every { storageStrategy.delete(meta.storagePath) } throws RuntimeException("delete failed")

    assertFalse(service.deleteFile(1L))
  }

  @Test
  fun `deleteFile throws when metadata missing`() {
    every { fileMetadataRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<NoSuchElementException> { service.deleteFile(1L) }
  }

  @Test
  fun `listFiles maps page to responses`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<FileMetadata> = PageImpl(listOf(metadata(1L), metadata(2L, "a.png")))
    every { fileMetadataRepository.findAll(pageable) } returns page
    every { storageStrategy.generateUrl(any(), any()) } returns "url"

    val result = service.listFiles(pageable)

    assertEquals(2, result.content.size)
    assertEquals("url", result.content[0].fileUrl)
  }

  @Test
  fun `listFilesByType filters by type`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<FileMetadata> = PageImpl(listOf(metadata()))
    every { fileMetadataRepository.findByFileType(FileType.IMAGE, pageable) } returns page

    val result = service.listFilesByType(FileType.IMAGE, pageable)

    assertEquals(1, result.content.size)
  }

  @Test
  fun `listFilesByOwner filters by owner`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<FileMetadata> = PageImpl(emptyList())
    every { fileMetadataRepository.findByOwnerId(1L, pageable) } returns page

    val result = service.listFilesByOwner(1L, pageable)

    assertEquals(0, result.content.size)
  }

  @Test
  fun `searchFiles searches by name`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<FileMetadata> = PageImpl(listOf(metadata()))
    every { fileMetadataRepository.findByFileNameContainingIgnoreCase("photo", pageable) } returns page

    val result = service.searchFiles("photo", pageable)

    assertEquals(1, result.content.size)
  }
}
