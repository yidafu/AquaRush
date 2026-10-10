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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.FileType
import java.time.LocalDateTime

/**
 * 文件元数据实体
 */
class FileMetadata {
  /**
   * 主键ID
   */
  var id: Long? = null

  /**
   * 原始文件名
   */
  var fileName: String = ""

  /**
   * 存储路径
   */
  var storagePath: String = ""

  /**
   * 文件类型
   */
  var fileType: FileType = FileType.OTHER

  /**
   * 文件大小（字节）
   */
  var fileSize: Long = 0

  /**
   * MIME类型
   */
  var mimeType: String = ""

  /**
   * 文件校验和 (SHA-256)
   */
  var checksum: String = ""

  /**
   * 创建时间
   */
  var createdAt: LocalDateTime = LocalDateTime.now()

  /**
   * 更新时间
   */
  var updatedAt: LocalDateTime = LocalDateTime.now()

  /**
   * 文件访问权限（公开/私有）
   */
  var isPublic: Boolean = true

  /**
   * 文件描述
   */
  var description: String? = null

  /**
   * 文件扩展名
   */
  var extension: String? = null

  /**
   * 文件所有者ID
   */
  var ownerId: Long? = null

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
    ownerId: Long? = null,
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

  // Equals and hashCode
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FileMetadata) return false
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String =
    "FileMetadata(id=$id, fileName='$fileName', storagePath='$storagePath', fileType=$fileType, fileSize=$fileSize, mimeType='$mimeType', checksum='$checksum', createdAt=$createdAt, updatedAt=$updatedAt, isPublic=$isPublic, description=$description, extension=$extension, ownerId=$ownerId)"
}
