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

package dev.yidafu.aqua.product.exception

/**
 * 产品未找到异常
 */
class ProductNotFoundException(productId: Long) :
    RuntimeException("Product not found with ID: $productId")

/**
 * 库存不足异常
 */
class InsufficientStockException(productId: Long, requested: Int, available: Int) :
    RuntimeException("Insufficient stock for product $productId. Requested: $requested, Available: $available")

/**
 * 无效价格异常
 */
class InvalidPriceException(price: Long, reason: String) :
    RuntimeException("Invalid price: $price. $reason")

/**
 * 产品状态转换异常
 */
class ProductStatusTransitionException(productId: Long, currentStatus: String, targetStatus: String) :
    RuntimeException("Invalid status transition for product $productId from $currentStatus to $targetStatus")

/**
 * 产品操作异常
 */
class ProductOperationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * 批量产品操作异常
 */
class BatchProductOperationException(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<String>
) : RuntimeException("Batch operation completed with $successCount successes and $failureCount failures. Failures: ${failures.joinToString(", ")}")