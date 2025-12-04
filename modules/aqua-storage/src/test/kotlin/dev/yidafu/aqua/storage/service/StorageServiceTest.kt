/*
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

package dev.yidafu.aqua.storage.service

import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.repository.FileMetadataRepository
import dev.yidafu.aqua.storage.service.impl.StorageServiceImpl
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import org.springframework.data.repository.findByIdOrNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.util.Optional

class StorageServiceTest {

    private lateinit var fileMetadataRepository: FileMetadataRepository
    private lateinit var storageStrategy: StorageStrategy
    private lateinit var imageProcessingService: ImageProcessingService
    private lateinit var storageProperties: StorageProperties
    private lateinit var storageService: StorageService

    @BeforeEach
    fun setUp() {
        fileMetadataRepository = mockk()
        storageStrategy = mockk()
        imageProcessingService = mockk()
        storageProperties = StorageProperties().apply {
            local.maxFileSize = 10L * 1024L * 1024L // 10MB
            local.allowedExtensions = setOf("jpg", "png", "pdf", "doc")
        }

        storageService = StorageServiceImpl(
            fileMetadataRepository = fileMetadataRepository,
            storageStrategy = storageStrategy,
            imageProcessingService = imageProcessingService,
            storageProperties = storageProperties,
            tika = org.apache.tika.Tika()
        )
    }

    @Test
    fun `should upload file successfully`() {
        // Given
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        val request = FileUploadRequest(
            fileType = FileType.IMAGE,
            description = "Test image",
            isPublic = true,
            ownerId = 1L
        )
        val storagePath = "images/2024/01/test_123456.jpg"
        val expectedMetadata = FileMetadata(
            fileName = "test.jpg",
            storagePath = storagePath,
            fileType = FileType.IMAGE,
            fileSize = file.size.toLong(),
            mimeType = "image/jpeg",
            checksum = "test_checksum",
            extension = "jpg",
            ownerId = 1L,
            isPublic = true,
            description = "Test image"
        ).apply { id = 1L }

        every { fileMetadataRepository.findByChecksum(any()) } returns Optional.empty()
        every { storageStrategy.store(file, any()) } returns storagePath
        every { fileMetadataRepository.save(any()) } returns expectedMetadata
        every { storageStrategy.generateUrl(1L) } returns "/api/v1/storage/files/1"

        // When
        val result = storageService.uploadFile(file, request)

        // Then
        assertNotNull(result)
        assertEquals("test.jpg", result.fileName)
        assertEquals(FileType.IMAGE, result.fileType)
        assertEquals("/api/v1/storage/files/1", result.fileUrl)
    }

    @Test
    fun `should throw exception for empty file`() {
        // Given
        val emptyFile = MockMultipartFile(
            "empty.jpg",
            "empty.jpg",
            "image/jpeg",
            ByteArray(0)
        )
        val request = FileUploadRequest()

        // When & Then
        assertThrows<IllegalArgumentException> {
            storageService.uploadFile(emptyFile, request)
        }
    }

    @Test
    fun `should throw exception for file size exceeded`() {
        // Given
        val largeFile = MockMultipartFile(
            "large.jpg",
            "large.jpg",
            "image/jpeg",
            ByteArray((20L * 1024L * 1024L).toInt()) // 20MB > 10MB limit
        )
        val request = FileUploadRequest()

        // When & Then
        assertThrows<IllegalArgumentException> {
            storageService.uploadFile(largeFile, request)
        }
    }

    @Test
    fun `should get file metadata successfully`() {
        // Given
        val fileId = 1L
        val storagePath = "images/2024/01/test.jpg"
        val metadata = FileMetadata(
            fileName = "test.jpg",
            storagePath = storagePath,
            fileType = FileType.IMAGE,
            fileSize = 1024L,
            mimeType = "image/jpeg",
            checksum = "test_checksum",
            extension = "jpg",
            isPublic = true
        ).apply { id = fileId }

        every { fileMetadataRepository.findByIdOrNull(fileId) } returns metadata
        every { storageStrategy.generateUrl(fileId) } returns "/api/v1/storage/files/$fileId"

        // When
        val result = storageService.getFileMetadata(fileId)

        // Then
        assertNotNull(result)
        assertEquals(fileId, result.id)
        assertEquals("test.jpg", result.fileName)
        assertEquals(FileType.IMAGE, result.fileType)
    }

    @Test
    fun `should throw exception when file not found`() {
        // Given
        val fileId = 999L
        every { fileMetadataRepository.findByIdOrNull(fileId) } returns null

        // When & Then
        assertThrows<NoSuchElementException> {
            storageService.getFileMetadata(fileId)
        }
    }

    @Test
    fun `should list files with pagination`() {
        // Given
        val pageable: Pageable = PageRequest.of(0, 20)
        val metadataList = listOf(
            FileMetadata(
                fileName = "test1.jpg",
                storagePath = "images/2024/01/test1.jpg",
                fileType = FileType.IMAGE,
                fileSize = 1024L,
                mimeType = "image/jpeg",
                checksum = "checksum1"
            ).apply { id = 1L },
            FileMetadata(
                fileName = "test2.jpg",
                storagePath = "images/2024/01/test2.jpg",
                fileType = FileType.IMAGE,
                fileSize = 2048L,
                mimeType = "image/jpeg",
                checksum = "checksum2"
            ).apply { id = 2L }
        )
        val page = PageImpl(metadataList)

        every { fileMetadataRepository.findAll(pageable) } returns page
        every { storageStrategy.generateUrl(any()) } returns "/api/v1/storage/files/test"

        // When
        val result = storageService.listFiles(pageable)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
    }
}
