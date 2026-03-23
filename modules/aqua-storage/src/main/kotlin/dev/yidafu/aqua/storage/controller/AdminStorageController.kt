package dev.yidafu.aqua.storage.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.storage.domain.enums.FileType
import dev.yidafu.aqua.storage.dto.FileMetadataResponse
import dev.yidafu.aqua.storage.service.StorageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/storage")
class AdminStorageController(
  private val storageService: StorageService,
) {
  // ========= 管理员请求 ================

  /**
   * 分页查询文件列表
   */
  @GetMapping("/files")
  fun listFiles(
    @RequestParam(value = "page", defaultValue = "0") page: Int,
    @RequestParam(value = "size", defaultValue = "20") size: Int,
    @RequestParam(value = "sort", defaultValue = "createdAt") sort: String,
    @RequestParam(value = "direction", defaultValue = "desc") direction: String,
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
    @RequestParam(value = "direction", defaultValue = "desc") direction: String,
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
    @RequestParam(value = "direction", defaultValue = "desc") direction: String,
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
    @RequestParam(value = "direction", defaultValue = "desc") direction: String,
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
    val fileTypes = FileType.entries
    return ResponseEntity.ok(ApiResponse.success(fileTypes))
  }

  /**
   * 健康检查
   */
  @GetMapping("/health")
  fun healthCheck(): ResponseEntity<ApiResponse<Map<String, String>>> =
    ResponseEntity.ok(
      ApiResponse.success(
        mapOf(
          "status" to "healthy",
          "service" to "aqua-storage",
          "timestamp" to System.currentTimeMillis().toString(),
        ),
      ),
    )
  // ========= 管理员请求 ================
}
