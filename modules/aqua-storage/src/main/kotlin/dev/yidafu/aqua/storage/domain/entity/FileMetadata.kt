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
class FileMetadata {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    /**
     * 原始文件名
     */
    @Column(nullable = false)
    var fileName: String = ""
        private set

    /**
     * 存储路径
     */
    @Column(nullable = false, unique = true)
    var storagePath: String = ""

    /**
     * 文件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var fileType: FileType = FileType.OTHER
        private set

    /**
     * 文件大小（字节）
     */
    @Column(nullable = false)
    var fileSize: Long = 0
        private set

    /**
     * MIME类型
     */
    @Column(nullable = false)
    var mimeType: String = ""
        private set

    /**
     * 文件校验和 (SHA-256)
     */
    @Column(nullable = false, unique = true)
    var checksum: String = ""
        private set

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        private set

    /**
     * 更新时间
     */
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        private set

    /**
     * 文件访问权限（公开/私有）
     */
    @Column(nullable = false)
    var isPublic: Boolean = true
        private set

    /**
     * 文件描述
     */
    @Column(length = 1000)
    var description: String? = null
        private set

    /**
     * 文件扩展名
     */
    @Column
    var extension: String? = null
        private set

    /**
     * 文件所有者ID
     */
    @Column
    var ownerId: Long? = null
        private set

    // No-arg constructor for Hibernate
    constructor()

    constructor(
        fileName: String,
        storagePath: String,
        fileType: FileType,
        fileSize: Long,
        mimeType: String,
        checksum: String,
        isPublic: Boolean = true,
        description: String? = null,
        extension: String? = null,
        ownerId: Long? = null
    ) {
        this.fileName = fileName
        this.storagePath = storagePath
        this.fileType = fileType
        this.fileSize = fileSize
        this.mimeType = mimeType
        this.checksum = checksum
        this.isPublic = isPublic
        this.description = description
        this.extension = extension
        this.ownerId = ownerId
        val now = LocalDateTime.now()
        this.createdAt = now
        this.updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    // Equals and hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileMetadata) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "FileMetadata(id=$id, fileName='$fileName', storagePath='$storagePath', fileType=$fileType, fileSize=$fileSize, mimeType='$mimeType', checksum='$checksum', createdAt=$createdAt, updatedAt=$updatedAt, isPublic=$isPublic, description=$description, extension=$extension, ownerId=$ownerId)"
    }
}
