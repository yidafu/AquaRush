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

package dev.yidafu.aqua.delivery.service

import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginRequest
import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginResponse

/**
 * Service interface for delivery worker authentication
 */
interface DeliveryAuthService {
  /**
   * Login with WeChat code
   * @return LoginResponse containing token and whether phone binding is needed
   */
  fun login(request: DeliveryLoginRequest): DeliveryLoginResponse

  /**
   * Bind phone number to delivery worker
   * @return LoginResponse with token after successful binding
   */
  fun bindPhone(
    openId: String,
    phoneNumber: String
  ): DeliveryLoginResponse
}
