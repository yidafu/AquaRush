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

package dev.yidafu.aqua.delivery.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.delivery.service.DeliveryAuthService
import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginRequest
import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for delivery worker authentication
 */
@RestController
@RequestMapping("/api/auth/delivery")
class DeliveryAuthController(
  private val deliveryAuthService: DeliveryAuthService
) {
  /**
   * Login with WeChat code
   * Returns token if already bound, otherwise returns needBindPhone=true
   */
  @PostMapping("/login")
  fun login(
    @RequestBody @Valid request: DeliveryLoginRequest
  ): ResponseEntity<ApiResponse<DeliveryLoginResponse>> {
    val response = deliveryAuthService.login(request)
    return ResponseEntity.ok(ApiResponse.success(response))
  }

  /**
   * Bind phone number to delivery worker
   * Requires openId from previous login and phone number from WeChat getPhoneNumber
   */
  @PostMapping("/bind-phone")
  fun bindPhone(
    @RequestBody @Valid request: BindPhoneRequest
  ): ResponseEntity<ApiResponse<DeliveryLoginResponse>> {
    val response = deliveryAuthService.bindPhone(request.openId, request.phoneNumber)
    return ResponseEntity.ok(ApiResponse.success(response))
  }
}

/**
 * Request DTO for binding phone number
 */
data class BindPhoneRequest(
  @NotBlank(message = "OpenID不能为空")
  val openId: String,
  @NotBlank(message = "手机号不能为空")
  val phoneNumber: String
)
