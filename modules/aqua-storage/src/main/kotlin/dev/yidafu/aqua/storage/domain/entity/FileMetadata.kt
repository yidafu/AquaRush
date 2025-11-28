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

package dev.yidafu.aqua.storage.domain.entity

import dev.yidafu.aqua.storage.domain.enums.FileType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 文件元数据实体
 */
@Entity
@Table(name = "file_metadata")
data class FileMetadata(
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 原始文件名
     */
    @Column(nullable = false)
    val fileName: String,

    /**
     * 存储路径
     */
    @Column(nullable = false, unique = true)
    var storagePath: String,

    /**
     * 文件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val fileType: FileType,

    /**
     * 文件大小（字节）
     */
    @Column(nullable = false)
    val fileSize: Long,

    /**
     * MIME类型
     */
    @Column(nullable = false)
    val mimeType: String,

    /**
     * 文件校验和 (SHA-256)
     */
    @Column(nullable = false, unique = true)
    val checksum: String,

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 更新时间
     */
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 文件访问权限（公开/私有）
     */
    @Column(nullable = false)
    val isPublic: Boolean = true,

    /**
     * 文件描述
     */
    @Column(length = 1000)
    var description: String? = null,

    /**
     * 文件扩展名
     */
    @Column
    val extension: String? = null,

    /**
     * 文件所有者ID
     */
    @Column
    val ownerId: Long? = null
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
