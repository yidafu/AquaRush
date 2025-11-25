package dev.yidafu.aqua.common.dto

/**
 * 分页请求参数
 */
data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null
)

/**
 * 分页响应
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
