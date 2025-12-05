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

package dev.yidafu.aqua.storage.dto

import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import java.time.LocalDateTime

/**
 * 文件元数据响应
 */
data class FileMetadataResponse(
    /**
     * 文件ID
     */
    val id: Long,

    /**
     * 原始文件名
     */
    val fileName: String,

    /**
     * 文件类型
     */
    val fileType: FileType,

    /**
     * 文件大小（字节）
     */
    val fileSize: Long,

    /**
     * MIME类型
     */
    val mimeType: String,

    /**
     * 文件扩展名
     */
    val extension: String?,

    /**
     * 创建时间
     */
    val createdAt: LocalDateTime,

    /**
     * 更新时间
     */
    val updatedAt: LocalDateTime,

    /**
     * 是否公开
     */
    val isPublic: Boolean,

    /**
     * 文件描述
     */
    val description: String?,

    /**
     * 文件所有者ID
     */
    val ownerId: Long?,

    /**
     * 文件访问URL
     */
    val fileUrl: String?,

    /**
     * 文件大小（可读格式）
     */
    val fileSizeFormatted: String
) {
    constructor(
      metadata: FileMetadata,
      fileUrl: String? = null
    ) : this(
        id = metadata.id,
        fileName = metadata.fileName,
        fileType = metadata.fileType,
        fileSize = metadata.fileSize,
        mimeType = metadata.mimeType,
        extension = metadata.extension,
        createdAt = metadata.createdAt,
        updatedAt = metadata.updatedAt,
        isPublic = metadata.isPublic,
        description = metadata.description,
        ownerId = metadata.ownerId,
        fileUrl = fileUrl,
        fileSizeFormatted = formatFileSize(metadata.fileSize)
    )

    companion object {
        /**
         * 格式化文件大小
         */
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "${bytes}B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "${String.format("%.1f", kb)}KB"
            val mb = kb / 1024.0
            if (mb < 1024) return "${String.format("%.1f", mb)}MB"
            val gb = mb / 1024.0
            return "${String.format("%.1f", gb)}GB"
        }
    }
}
