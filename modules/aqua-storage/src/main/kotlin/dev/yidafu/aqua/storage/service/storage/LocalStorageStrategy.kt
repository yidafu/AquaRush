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
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * 本地文件系统存储策略实现
 */
@Component
class LocalStorageStrategy(
    private val storageProperties: StorageProperties
) : StorageStrategy {

    private val basePath: Path = Paths.get(storageProperties.local.basePath)

    init {
        // 确保基础存储目录存在
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath)
        }
    }

    override fun store(file: MultipartFile, metadata: FileMetadata): String {
        return try {
            // 按年月创建目录结构
            val targetDir = basePath.resolve(generatePathByFileType(metadata.fileType))
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir)
            }

            // 生成唯一文件名
            val fileName = generateUniqueFileName(file.originalFilename ?: "unknown")
            val targetPath = targetDir.resolve(fileName)

            // 存储文件
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // 返回相对路径
            generateRelativePath(metadata.fileType, fileName)
        } catch (e: IOException) {
            throw RuntimeException("Failed to store file", e)
        }
    }

    override fun retrieve(path: String): Resource {
        val fullPath = basePath.resolve(path)
        val file = fullPath.toFile()

        if (!file.exists()) {
            throw RuntimeException("File not found: $path")
        }

        return FileSystemResource(file)
    }

    override fun delete(path: String): Boolean {
        return try {
            val fullPath = basePath.resolve(path)
            Files.deleteIfExists(fullPath)
        } catch (e: IOException) {
            false
        }
    }

    override fun generateUrl(fileId: Long, filename: String): String {
        // 生成文件下载URL，使用文件ID访问下载端点
        // 格式：{baseUrl}/api/v1/storage/files/{id}
        return "${storageProperties.local.baseUrl}/api/v1/storage/files/$fileId?name=$filename"
    }

    override fun exists(path: String): Boolean {
        val fullPath = basePath.resolve(path)
        return Files.exists(fullPath)
    }

    override fun getFileSize(path: String): Long {
        val fullPath = basePath.resolve(path)
        return try {
            Files.size(fullPath)
        } catch (e: IOException) {
            0L
        }
    }

    /**
     * 根据文件类型生成存储路径
     */
    private fun generatePathByFileType(fileType: dev.yidafu.aqua.storage.domain.enums.FileType): String {
        val now = java.time.LocalDateTime.now()
        return "${fileType.name.lowercase()}/${now.year}/${String.format("%02d", now.monthValue)}"
    }

    /**
     * 生成唯一文件名
     */
    private fun generateUniqueFileName(originalName: String): String {
        val extension = if (originalName.contains('.')) {
            originalName.substringAfterLast('.')
        } else {
            ""
        }

        val timestamp = System.currentTimeMillis()
        val randomString = (1..6).map { ('a'..'z').random() }.joinToString("")

        return if (extension.isNotEmpty()) {
            "${timestamp}_${randomString}.$extension"
        } else {
            "${timestamp}_$randomString"
        }
    }

    /**
     * 生成相对路径
     */
    private fun generateRelativePath(fileType: dev.yidafu.aqua.storage.domain.enums.FileType, fileName: String): String {
        return "${generatePathByFileType(fileType)}/$fileName"
    }
}
