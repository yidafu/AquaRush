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

package dev.yidafu.aqua.storage.service

import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

/**
 * 存储服务接口
 */
interface StorageService {
    /**
     * 上传文件
     * @param file 要上传的文件
     * @param request 上传请求参数
     * @return 文件元数据
     */
    fun uploadFile(file: MultipartFile, request: FileUploadRequest): FileMetadataResponse

    /**
     * 根据ID获取文件
     * @param id 文件ID
     * @return 文件资源
     */
    fun getFile(id: Long): Resource

    /**
     * 根据ID获取处理后的图片
     * @param id 文件ID
     * @param parameters 图片处理参数
     * @return 处理后的图片资源
     */
    fun getProcessedImage(id: Long, parameters: ImageParameters): ByteArray

    /**
     * 根据ID获取文件元数据
     * @param id 文件ID
     * @return 文件元数据
     */
    fun getFileMetadata(id: Long): FileMetadataResponse

    /**
     * 删除文件
     * @param id 文件ID
     * @return 是否删除成功
     */
    fun deleteFile(id: Long): Boolean

    /**
     * 分页查询文件元数据
     * @param pageable 分页参数
     * @return 文件元数据分页
     */
    fun listFiles(pageable: Pageable): Page<FileMetadataResponse>

    /**
     * 根据文件类型查询文件
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文件元数据分页
     */
    fun listFilesByType(fileType: dev.yidafu.aqua.storage.domain.enums.FileType, pageable: Pageable): Page<FileMetadataResponse>

    /**
     * 根据所有者查询文件
     * @param ownerId 所有者ID
     * @param pageable 分页参数
     * @return 文件元数据分页
     */
    fun listFilesByOwner(ownerId: Long?, pageable: Pageable): Page<FileMetadataResponse>

    /**
     * 根据文件名搜索文件
     * @param fileName 文件名（支持模糊搜索）
     * @param pageable 分页参数
     * @return 文件元数据分页
     */
    fun searchFiles(fileName: String, pageable: Pageable): Page<FileMetadataResponse>
}
