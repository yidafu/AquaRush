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

package dev.yidafu.aqua.storage.service.impl

import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.repository.FileMetadataRepository
import dev.yidafu.aqua.storage.service.ImageProcessingService
import dev.yidafu.aqua.storage.service.StorageService
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import org.apache.tika.Tika
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.security.MessageDigest

/**
 * 存储服务实现
 */
@Service
@Transactional
class StorageServiceImpl(
    private val fileMetadataRepository: FileMetadataRepository,
    private val storageStrategy: StorageStrategy,
    private val imageProcessingService: ImageProcessingService,
    private val storageProperties: StorageProperties,
    private val tika: Tika
) : StorageService {

    override fun uploadFile(file: MultipartFile, request: FileUploadRequest): FileMetadataResponse {
        if (file.isEmpty) {
            throw IllegalArgumentException("File cannot be empty")
        }

        // 检查文件大小
        if (file.size > storageProperties.local.maxFileSize) {
            throw IllegalArgumentException("File size exceeds maximum allowed size")
        }

        // 检查文件扩展名
        val fileName = file.originalFilename ?: "unknown"
        val extension = getFileExtension(fileName)
        if (extension != null && !storageProperties.local.allowedExtensions.contains(extension.lowercase())) {
            throw IllegalArgumentException("File extension not allowed: $extension")
        }

        try {
            // 计算文件校验和
            val checksum = calculateChecksum(file.bytes)

            // 检查是否已存在相同文件
            val existingFile = fileMetadataRepository.findByChecksum(checksum).orElse(null)
            if (existingFile != null) {
                val fileUrl = storageStrategy.generateUrl(existingFile.storagePath)
                return FileMetadataResponse(existingFile, fileUrl)
            }

            // 检测MIME类型和文件类型
            val mimeType = tika.detect(file.inputStream, fileName)
            val fileType = detectFileType(fileName, mimeType, request.fileType)

            // 创建文件元数据
            val fileMetadata = FileMetadata(
                fileName = fileName,
                storagePath = "", // 将在存储后更新
                fileType = fileType,
                fileSize = file.size,
                mimeType = mimeType,
                checksum = checksum,
                isPublic = request.isPublic,
                description = request.description,
                extension = extension,
                ownerId = request.ownerId
            )

            // 存储文件
            val storagePath = storageStrategy.store(file, fileMetadata)
            fileMetadata.storagePath = storagePath

            // 保存元数据
            val savedMetadata = fileMetadataRepository.save(fileMetadata)

            val fileUrl = storageStrategy.generateUrl(storagePath)
            return FileMetadataResponse(savedMetadata, fileUrl)
        } catch (e: IOException) {
            throw RuntimeException("Failed to upload file", e)
        }
    }

    override fun getFile(id: Long): Resource {
        val metadata = fileMetadataRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("File not found with id: $id")

        return storageStrategy.retrieve(metadata.storagePath)
    }

    override fun getProcessedImage(id: Long, parameters: ImageParameters): ByteArray {
        val metadata = fileMetadataRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("File not found with id: $id")

        if (metadata.fileType != FileType.IMAGE) {
            throw IllegalArgumentException("File is not an image: ${metadata.fileType}")
        }

        return imageProcessingService.getOrProcessImage(metadata.storagePath, parameters)
    }

    override fun getFileMetadata(id: Long): FileMetadataResponse {
        val metadata = fileMetadataRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("File not found with id: $id")

        val fileUrl = storageStrategy.generateUrl(metadata.storagePath)
        return FileMetadataResponse(metadata, fileUrl)
    }

    override fun deleteFile(id: Long): Boolean {
        val metadata = fileMetadataRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("File not found with id: $id")

        return try {
            // 删除物理文件
            storageStrategy.delete(metadata.storagePath)
            // 删除元数据
            fileMetadataRepository.delete(metadata)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun listFiles(pageable: org.springframework.data.domain.Pageable): Page<FileMetadataResponse> {
        return fileMetadataRepository.findAll(pageable).map { metadata ->
            val fileUrl = storageStrategy.generateUrl(metadata.storagePath)
            FileMetadataResponse(metadata, fileUrl)
        }
    }

    override fun listFilesByType(fileType: FileType, pageable: org.springframework.data.domain.Pageable): Page<FileMetadataResponse> {
        return fileMetadataRepository.findByFileType(fileType, pageable).map { metadata ->
            val fileUrl = storageStrategy.generateUrl(metadata.storagePath)
            FileMetadataResponse(metadata, fileUrl)
        }
    }

    override fun listFilesByOwner(ownerId: Long?, pageable: org.springframework.data.domain.Pageable): Page<FileMetadataResponse> {
        return fileMetadataRepository.findByOwnerId(ownerId, pageable).map { metadata ->
            val fileUrl = storageStrategy.generateUrl(metadata.storagePath)
            FileMetadataResponse(metadata, fileUrl)
        }
    }

    override fun searchFiles(fileName: String, pageable: org.springframework.data.domain.Pageable): Page<FileMetadataResponse> {
        return fileMetadataRepository.findByFileNameContainingIgnoreCase(fileName, pageable).map { metadata ->
            val fileUrl = storageStrategy.generateUrl(metadata.storagePath)
            FileMetadataResponse(metadata, fileUrl)
        }
    }

    /**
     * 计算文件校验和
     */
    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(fileName: String): String? {
        return if (fileName.contains('.')) {
            fileName.substringAfterLast('.').lowercase()
        } else {
            null
        }
    }

    /**
     * 检测文件类型
     */
    private fun detectFileType(fileName: String, mimeType: String, requestedType: FileType?): FileType {
        // 如果明确指定了文件类型，优先使用
        requestedType?.let { return it }

        // 根据MIME类型检测
        return when {
            mimeType.startsWith("image/") -> FileType.IMAGE
            mimeType.startsWith("video/") -> FileType.VIDEO
            mimeType.startsWith("audio/") -> FileType.AUDIO
            mimeType.contains("pdf") -> FileType.DOCUMENT
            mimeType.contains("word") || mimeType.contains("document") -> FileType.DOCUMENT
            mimeType.contains("excel") || mimeType.contains("spreadsheet") -> FileType.SPREADSHEET
            mimeType.contains("powerpoint") || mimeType.contains("presentation") -> FileType.PRESENTATION
            mimeType.contains("text/") -> FileType.DOCUMENT
            mimeType.contains("html") || mimeType.contains("css") || mimeType.contains("javascript") -> FileType.FRONTEND
            mimeType.contains("zip") || mimeType.contains("compressed") -> FileType.ARCHIVE
            mimeType.contains("executable") -> FileType.EXECUTABLE
            else -> FileType.OTHER
        }
    }
}
