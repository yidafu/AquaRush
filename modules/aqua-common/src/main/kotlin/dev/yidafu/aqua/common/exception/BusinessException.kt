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

package dev.yidafu.aqua.common.exception

/**
 * Base business exception
 */
open class BusinessException(
  override val message: String,
  val code: String = "BUSINESS_ERROR",
) : RuntimeException(message)

class NotFoundException(
  message: String,
) : BusinessException(message, "NOT_FOUND")

class BadRequestException(
  message: String,
) : BusinessException(message, "BAD_REQUEST")

class UnauthorizedException(
  message: String,
) : BusinessException(message, "UNAUTHORIZED")

class ForbiddenException(
  message: String,
) : BusinessException(message, "FORBIDDEN")

class InsufficientStockException(
  message: String,
) : BusinessException(message, "INSUFFICIENT_STOCK")
