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

import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

/**
 * 存储策略接口
 */
interface StorageStrategy {
    /**
     * 存储文件
     * @param file 要存储的文件
     * @param metadata 文件元数据
     * @return 存储路径
     */
    fun store(file: MultipartFile, metadata: FileMetadata): String

    /**
     * 获取文件
     * @param path 文件路径
     * @return 文件资源
     */
    fun retrieve(path: String): Resource

    /**
     * 删除文件
     * @param path 文件路径
     * @return 是否删除成功
     */
    fun delete(path: String): Boolean

    /**
     * 生成文件访问URL
     * @param path 文件路径
     * @return 文件URL
     */
    fun generateUrl(path: String): String

    /**
     * 检查文件是否存在
     * @param path 文件路径
     * @return 文件是否存在
     */
    fun exists(path: String): Boolean

    /**
     * 获取文件大小
     * @param path 文件路径
     * @return 文件大小（字节）
     */
    fun getFileSize(path: String): Long
}
