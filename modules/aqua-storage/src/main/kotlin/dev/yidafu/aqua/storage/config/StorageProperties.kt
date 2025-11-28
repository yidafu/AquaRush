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

package dev.yidafu.aqua.storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 存储配置属性
 */
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    /**
     * 存储类型：local, s3, oss
     */
    var type: StorageType = StorageType.LOCAL,

    /**
     * 本地存储配置
     */
    var local: LocalStorageProperties = LocalStorageProperties(),

    /**
     * S3存储配置
     */
    var s3: S3StorageProperties = S3StorageProperties(),

    /**
     * OSS存储配置
     */
    var oss: OSSStorageProperties = OSSStorageProperties()
) {
    /**
     * 存储类型枚举
     */
    enum class StorageType {
        LOCAL, S3, OSS
    }
}

/**
 * 本地存储配置
 */
data class LocalStorageProperties(
    /**
     * 基础存储路径
     */
    var basePath: String = "/var/aqua/storage",

    /**
     * 最大文件大小（字节）
     */
    var maxFileSize: Long = 100L * 1024L * 1024L, // 100MB

    /**
     * 允许的文件扩展名
     */
    var allowedExtensions: Set<String> = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp",
                                           "mp4", "avi", "mov", "wmv", "flv",
                                           "mp3", "wav", "flac", "aac",
                                           "pdf", "doc", "docx", "txt",
                                           "xls", "xlsx", "csv",
                                           "ppt", "pptx",
                                           "html", "css", "js",
                                           "zip", "rar", "7z", "tar", "gz",
                                           "exe", "msi", "sh", "bat",
                                           "bak", "backup")
)

/**
 * S3存储配置
 */
data class S3StorageProperties(
    /**
     * 存储桶名称
     */
    var bucket: String = "aqua-storage",

    /**
     * AWS区域
     */
    var region: String = "us-west-1",

    /**
     * 访问密钥
     */
    var accessKey: String = "",

    /**
     * 秘密密钥
     */
    var secretKey: String = "",

    /**
     * 端点URL（可选）
     */
    var endpoint: String? = null
)

/**
 * OSS存储配置
 */
data class OSSStorageProperties(
    /**
     * 存储桶名称
     */
    var bucket: String = "aqua-storage",

    /**
     * 端点URL
     */
    var endpoint: String = "oss-cn-hangzhou.aliyuncs.com",

    /**
     * 访问密钥
     */
    var accessKey: String = "",

    /**
     * 秘密密钥
     */
    var secretKey: String = ""
)
