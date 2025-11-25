package dev.yidafu.aqua.common

/**
 * Common result wrapper for API responses
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val code: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun <T> error(message: String, code: String? = null): ApiResponse<T> {
            return ApiResponse(success = false, message = message, code = code)
        }
    }
}
