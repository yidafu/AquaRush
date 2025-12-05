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

package dev.yidafu.aqua.storage.service.storage

import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class LocalStorageStrategyTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storageProperties: StorageProperties
    private lateinit var localStorageStrategy: LocalStorageStrategy

    @BeforeEach
    fun setUp() {
        storageProperties = StorageProperties().apply {
            local.basePath = tempDir.toString()
        }
        localStorageStrategy = LocalStorageStrategy(storageProperties)
    }

    @Test
    fun `should store file successfully`() {
        // Given
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        val metadata = FileMetadata(
            fileName = "test.jpg",
            storagePath = "", // Will be set by storage strategy
            fileType = FileType.IMAGE,
            fileSize = file.size.toLong(),
            mimeType = "image/jpeg",
            checksum = "test_checksum",
            extension = "jpg"
        ).apply { id = 1L }

        // When
        val storagePath = localStorageStrategy.store(file, metadata)

        // Then
        assertNotNull(storagePath)
        assertTrue(storagePath.isNotEmpty())
        assertTrue(Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `should retrieve stored file`() {
        // Given
        val fileContent = "test image content"
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            fileContent.toByteArray()
        )
        val metadata = FileMetadata(
            fileName = "test.jpg",
            storagePath = "",
            fileType = FileType.IMAGE,
            fileSize = file.size.toLong(),
            mimeType = "image/jpeg",
            checksum = "test_checksum",
            extension = "jpg"
        ).apply { id = 1L }

        // When
        val storagePath = localStorageStrategy.store(file, metadata)
        val resource = localStorageStrategy.retrieve(storagePath)

        // Then
        assertNotNull(resource)
        assertTrue(resource.exists())
        assertArrayEquals(fileContent.toByteArray(), resource.inputStream.readBytes())
    }

    @Test
    fun `should delete stored file`() {
        // Given
        val file = MockMultipartFile(
            "test.jpg",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        val metadata = FileMetadata(
            fileName = "test.jpg",
            storagePath = "",
            fileType = FileType.IMAGE,
            fileSize = file.size.toLong(),
            mimeType = "image/jpeg",
            checksum = "test_checksum",
            extension = "jpg"
        ).apply { id = 1L }

        // When
        val storagePath = localStorageStrategy.store(file, metadata)
        assertTrue(Files.exists(tempDir.resolve(storagePath)))

        val deleted = localStorageStrategy.delete(storagePath)

        // Then
        assertTrue(deleted)
        assertFalse(Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `should check file existence`() {
        // Given
        val storagePath = "nonexistent/file.jpg"

        // When
        val exists = localStorageStrategy.exists(storagePath)

        // Then
        assertFalse(exists)
    }

    @Test
    fun `should generate URL`() {
        // Given
        val fileId = 123L

        // When
        val url = localStorageStrategy.generateUrl(fileId)

        // Then
        assertNotNull(url)
        assertTrue(url.contains(fileId.toString()))
        assertEquals("http://localhost:8080/api/v1/storage/files/$fileId", url)
    }

    @Test
    fun `should organize files by type and date`() {
        // Given
        val imageFile = MockMultipartFile(
            "image.jpg",
            "image.jpg",
            "image/jpeg",
            "image content".toByteArray()
        )
        val imageMetadata = FileMetadata(
            fileName = "image.jpg",
            storagePath = "",
            fileType = FileType.IMAGE,
            fileSize = imageFile.size.toLong(),
            mimeType = "image/jpeg",
            checksum = "image_checksum",
            extension = "jpg"
        ).apply { id = 1L }

        val documentFile = MockMultipartFile(
            "document.pdf",
            "document.pdf",
            "application/pdf",
            "document content".toByteArray()
        )
        val documentMetadata = FileMetadata(
            fileName = "document.pdf",
            storagePath = "",
            fileType = FileType.DOCUMENT,
            fileSize = documentFile.size.toLong(),
            mimeType = "application/pdf",
            checksum = "document_checksum",
            extension = "pdf"
        ).apply { id = 2L }

        // When
        val imageStoragePath = localStorageStrategy.store(imageFile, imageMetadata)
        val documentStoragePath = localStorageStrategy.store(documentFile, documentMetadata)

        // Then
        assertTrue(imageStoragePath.startsWith("image/"))
        assertTrue(documentStoragePath.startsWith("document/"))
        assertTrue(Files.exists(tempDir.resolve(imageStoragePath)))
        assertTrue(Files.exists(tempDir.resolve(documentStoragePath)))
    }
}
