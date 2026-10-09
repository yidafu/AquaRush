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

import dev.yidafu.aqua.common.domain.model.FileMetadata
import dev.yidafu.aqua.common.domain.model.enums.FileType
import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.repository.FileMetadataRepository
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.service.ImageProcessingService
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.tika.Tika
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDateTime
import java.util.Optional

/**
 * 存储服务单元测试: 上传校验(空/大小/扩展名/去重)、MIME 检测、读写删、图片处理路由
 */
class StorageServiceImplTest :
  FunSpec({
    var fileMetadataRepository: FileMetadataRepository = mockk()
    var storageStrategy: StorageStrategy = mockk()
    var imageProcessingService: ImageProcessingService = mockk()
    var storageProperties: StorageProperties = mockk()
    var tika: Tika = mockk()
    var service: StorageServiceImpl = mockk()

    fun fakeFile(
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

    fun metadata(
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

    beforeTest {
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
    test("uploadFile rejects empty file") {
      val file = fakeFile(bytes = byteArrayOf())

      shouldThrow<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
    }
    test("uploadFile rejects oversized file") {
      val file = fakeFile(bytes = ByteArray(2048))

      shouldThrow<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
    }
    test("uploadFile rejects disallowed extension") {
      val file = fakeFile(name = "virus.exe")

      shouldThrow<IllegalArgumentException> { service.uploadFile(file, FileUploadRequest()) }
    }
    test("uploadFile returns existing file by checksum without storing again") {
      val existing = metadata()
      every { fileMetadataRepository.findByChecksum(any()) } returns Optional.of(existing)
      every { storageStrategy.generateUrl(existing.id!!, existing.fileName) } returns
        "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"

      val response = service.uploadFile(fakeFile(), FileUploadRequest())

      response.id shouldBe existing.id
      response.fileUrl shouldBe "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"
      verify(exactly = 0) { storageStrategy.store(any(), any()) }
    }
    test("uploadFile stores new file and saves metadata") {
      every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
      every { tika.detect(any<java.io.InputStream>(), any<String>()) } returns "image/png"
      every { storageStrategy.store(any(), any()) } returns "image/2025/06/photo.png"
      every { storageStrategy.generateUrl(any(), any()) } returns "http://localhost:9090/api/v1/storage/files/1?name=photo.png"
      every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

      val response = service.uploadFile(fakeFile(name = "photo.png", contentType = "image/png"), FileUploadRequest())

      response.fileName shouldBe "photo.png"
      response.fileType shouldBe FileType.IMAGE
      verify { storageStrategy.store(any(), any()) }
      verify { fileMetadataRepository.save(any()) }
    }
    test("uploadFile detects file type from mime") {
      every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
      every { tika.detect(any<java.io.InputStream>(), any<String>()) } returns "application/pdf"
      every { storageStrategy.store(any(), any()) } returns "document/2025/06/doc.pdf"
      every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

      val response = service.uploadFile(fakeFile(name = "doc.txt"), FileUploadRequest())

      response.fileType shouldBe FileType.DOCUMENT
    }
    test("uploadFile honors requested file type") {
      every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
      every { storageStrategy.store(any(), any()) } returns "archive/2025/06/data.zip"
      every { fileMetadataRepository.save(any()) } answers { (firstArg<FileMetadata>()).also { it.id = 1L } }

      val response = service.uploadFile(fakeFile(name = "data.txt"), FileUploadRequest(fileType = FileType.ARCHIVE))

      response.fileType shouldBe FileType.ARCHIVE
    }
    test("uploadFile wraps io exception in runtime exception") {
      every { fileMetadataRepository.findByChecksum(any()) } throws IOException("disk error")

      shouldThrow<RuntimeException> { service.uploadFile(fakeFile(), FileUploadRequest()) }
    }
    test("getFile returns resource for existing metadata") {
      val meta = metadata()
      val resource = ByteArrayResource(byteArrayOf(9))
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
      every { storageStrategy.retrieve(meta.storagePath) } returns resource

      resource.shouldBeSameInstanceAs(service.getFile(1L))
    }
    test("getFile throws when metadata missing") {
      every { fileMetadataRepository.findById(1L) } returns Optional.empty()

      shouldThrow<NoSuchElementException> { service.getFile(1L) }
    }
    test("getProcessedImage returns processed bytes for image") {
      val meta = metadata()
      val params = ImageParameters(width = 100)
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
      every { imageProcessingService.getOrProcessImage(meta.storagePath, params) } returns byteArrayOf(1, 2)

      val bytes = service.getProcessedImage(1L, params)

      bytes.size shouldBe 2
    }
    test("getProcessedImage rejects non-image file") {
      val meta = metadata(fileType = FileType.DOCUMENT, extension = "pdf")
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)

      shouldThrow<IllegalArgumentException> { service.getProcessedImage(1L, ImageParameters(width = 100)) }
    }
    test("getFileMetadata returns response with url") {
      val meta = metadata()
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
      every { storageStrategy.generateUrl(1L, meta.fileName) } returns "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"

      val response = service.getFileMetadata(1L)

      response.id shouldBe 1L
      response.fileUrl shouldBe "http://localhost:9090/api/v1/storage/files/1?name=photo.jpg"
    }
    test("deleteFile deletes storage and metadata") {
      val meta = metadata()
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
      every { storageStrategy.delete(meta.storagePath) } returns true

      service.deleteFile(1L) shouldBe true
      verify { fileMetadataRepository.delete(meta) }
    }
    test("deleteFile returns false when storage delete fails") {
      val meta = metadata()
      every { fileMetadataRepository.findById(1L) } returns Optional.of(meta)
      every { storageStrategy.delete(meta.storagePath) } throws RuntimeException("delete failed")

      service.deleteFile(1L) shouldBe false
    }
    test("deleteFile throws when metadata missing") {
      every { fileMetadataRepository.findById(1L) } returns Optional.empty()

      shouldThrow<NoSuchElementException> { service.deleteFile(1L) }
    }
    test("listFiles maps page to responses") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<FileMetadata> = PageImpl(listOf(metadata(1L), metadata(2L, "a.png")))
      every { fileMetadataRepository.findAll(pageable) } returns page
      every { storageStrategy.generateUrl(any(), any()) } returns "url"

      val result = service.listFiles(pageable)

      result.content.size shouldBe 2
      result.content[0].fileUrl shouldBe "url"
    }
    test("listFilesByType filters by type") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<FileMetadata> = PageImpl(listOf(metadata()))
      every { fileMetadataRepository.findByFileType(FileType.IMAGE, pageable) } returns page

      val result = service.listFilesByType(FileType.IMAGE, pageable)

      result.content.size shouldBe 1
    }
    test("listFilesByOwner filters by owner") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<FileMetadata> = PageImpl(emptyList())
      every { fileMetadataRepository.findByOwnerId(1L, pageable) } returns page

      val result = service.listFilesByOwner(1L, pageable)

      result.content.size shouldBe 0
    }
    test("searchFiles searches by name") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<FileMetadata> = PageImpl(listOf(metadata()))
      every { fileMetadataRepository.findByFileNameContainingIgnoreCase("photo", pageable) } returns page

      val result = service.searchFiles("photo", pageable)

      result.content.size shouldBe 1
    }
  })
