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

import dev.yidafu.aqua.storage.domain.enums.FileType

/**
 * 文件上传请求
 */
data class FileUploadRequest(
    /**
     * 文件类型（可选，如果不提供则自动检测）
     */
    val fileType: FileType? = null,

    /**
     * 文件描述
     */
    val description: String? = null,

    /**
     * 是否公开（默认为true）
     */
    val isPublic: Boolean = true,

    /**
     * 文件所有者ID（可选）
     */
    val ownerId: Long? = null
)
