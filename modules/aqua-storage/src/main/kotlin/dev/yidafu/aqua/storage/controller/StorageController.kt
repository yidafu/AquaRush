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

package dev.yidafu.aqua.storage.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.dto.FileUploadRequest
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.domain.enums.FileType
import dev.yidafu.aqua.storage.service.StorageService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 存储控制器
 */
@RestController
@RequestMapping("/api/v1/storage")
class StorageController(
    private val storageService: StorageService
) {

    /**
     * 文件上传
     */
    @PostMapping("/files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("fileType", required = false) fileType: FileType?,
        @RequestParam("description", required = false) description: String?,
        @RequestParam(value = "isPublic", defaultValue = "true") isPublic: Boolean,
        @RequestParam("ownerId", required = false) ownerId: Long?
    ): ApiResponse<FileMetadataResponse> {
        val request = FileUploadRequest(
            fileType = fileType,
            description = description,
            isPublic = isPublic,
            ownerId = ownerId
        )

        val result = storageService.uploadFile(file, request)
        return ApiResponse.success(result)
    }

    /**
     * 获取文件
     */
    @GetMapping("/files/{id}")
    fun getFile(
        @PathVariable id: Long,
        @RequestParam(value = "name", required = false) name: String?,
        response: HttpServletResponse
    ): ResponseEntity<Resource> {
        val resource = storageService.getFile(id)
        val metadata = storageService.getFileMetadata(id)

        // 使用请求参数中的文件名，如果没有则使用元数据中的文件名
        val fileName = name ?: metadata.fileName

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(metadata.mimeType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$fileName\"")
            .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 缓存1年
            .body(resource)
    }

    /**
     * 获取处理后的图片
     */
    @GetMapping("/files/{id}/image")
    fun getProcessedImage(
        @PathVariable id: Long,
        @RequestParam(value = "width", required = false) width: Int?,
        @RequestParam(value = "height", required = false) height: Int?,
        @RequestParam(value = "quality", required = false) quality: Float?,
        @RequestParam(value = "format", required = false) format: String?,
        @RequestParam(value = "watermark", defaultValue = "false") watermark: Boolean,
        @RequestParam(value = "watermarkText", required = false) watermarkText: String?,
        response: HttpServletResponse
    ): ResponseEntity<ByteArray> {
        val parameters = ImageParameters(
            width = width,
            height = height,
            quality = quality,
            format = format,
            watermark = watermark,
            watermarkText = watermarkText
        )

        val processedImage = storageService.getProcessedImage(id, parameters)
        val metadata = storageService.getFileMetadata(id)

        // 确定响应的MIME类型
        val responseFormat = format?.uppercase() ?: "JPEG"
        val mimeType = when (responseFormat) {
            "PNG" -> "image/png"
            "WEBP" -> "image/webp"
            "GIF" -> "image/gif"
            "BMP" -> "image/bmp"
            else -> "image/jpeg"
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mimeType))
            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400") // 缓存1天
            .body(processedImage)
    }

    /**
     * 获取文件元数据
     */
    @GetMapping("/files/{id}/metadata")
    fun getFileMetadata(@PathVariable id: Long): ResponseEntity<ApiResponse<FileMetadataResponse>> {
        val metadata = storageService.getFileMetadata(id)
        return ResponseEntity.ok(ApiResponse.success(metadata))
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/files/{id}")
    fun deleteFile(@PathVariable id: Long): ResponseEntity<ApiResponse<Boolean>> {
        val result = storageService.deleteFile(id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 分页查询文件列表
     */
    @GetMapping("/files")
    fun listFiles(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @RequestParam(value = "sort", defaultValue = "createdAt") sort: String,
        @RequestParam(value = "direction", defaultValue = "desc") direction: String
    ): ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> {
        val sortDirection = if (direction.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        val result = storageService.listFiles(pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 根据文件类型查询文件
     */
    @GetMapping("/files/by-type/{fileType}")
    fun listFilesByType(
        @PathVariable fileType: FileType,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @RequestParam(value = "sort", defaultValue = "createdAt") sort: String,
        @RequestParam(value = "direction", defaultValue = "desc") direction: String
    ): ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> {
        val sortDirection = if (direction.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        val result = storageService.listFilesByType(fileType, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 根据所有者查询文件
     */
    @GetMapping("/files/by-owner/{ownerId}")
    fun listFilesByOwner(
        @PathVariable ownerId: Long?,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @RequestParam(value = "sort", defaultValue = "createdAt") sort: String,
        @RequestParam(value = "direction", defaultValue = "desc") direction: String
    ): ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> {
        val sortDirection = if (direction.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        val result = storageService.listFilesByOwner(ownerId, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 搜索文件
     */
    @GetMapping("/files/search")
    fun searchFiles(
        @RequestParam("q") fileName: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
        @RequestParam(value = "sort", defaultValue = "createdAt") sort: String,
        @RequestParam(value = "direction", defaultValue = "desc") direction: String
    ): ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> {
        val sortDirection = if (direction.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        val result = storageService.searchFiles(fileName, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 获取支持的文件类型列表
     */
    @GetMapping("/file-types")
    fun getSupportedFileTypes(): ResponseEntity<ApiResponse<List<FileType>>> {
        val fileTypes = FileType.values().toList()
        return ResponseEntity.ok(ApiResponse.success(fileTypes))
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<ApiResponse<Map<String, String>>> {
        return ResponseEntity.ok(ApiResponse.success(mapOf(
            "status" to "healthy",
            "service" to "aqua-storage",
            "timestamp" to System.currentTimeMillis().toString()
        )))
    }

    /**
     * 异常处理
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value().toString(), ex.message ?: "请求参数错误"))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(HttpStatus.NOT_FOUND.value().toString(), ex.message ?: "文件不存在"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ApiResponse<Nothing>> {
        // 如果错误消息包含文件路径信息，提供更友好的错误信息
        val message = when {
            ex.message?.contains("File not found") == true -> "文件不存在或已被删除"
            ex.message?.contains("Failed to store file") == true -> "文件存储失败，请重试"
            else -> ex.message ?: "服务器内部错误"
        }
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value().toString(), message))
    }
}
