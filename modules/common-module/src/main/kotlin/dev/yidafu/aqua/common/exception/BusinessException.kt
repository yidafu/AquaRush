package dev.yidafu.aqua.common.exception

/**
 * Base business exception
 */
open class BusinessException(
    override val message: String,
    val code: String = "BUSINESS_ERROR"
) : RuntimeException(message)

class NotFoundException(message: String) : BusinessException(message, "NOT_FOUND")

class BadRequestException(message: String) : BusinessException(message, "BAD_REQUEST")

class UnauthorizedException(message: String) : BusinessException(message, "UNAUTHORIZED")

class ForbiddenException(message: String) : BusinessException(message, "FORBIDDEN")

class InsufficientStockException(message: String) : BusinessException(message, "INSUFFICIENT_STOCK")
