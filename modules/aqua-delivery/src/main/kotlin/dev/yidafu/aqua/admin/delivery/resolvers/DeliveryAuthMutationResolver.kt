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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.dto.DeliveryLoginRequest
import dev.yidafu.aqua.api.dto.DeliveryLoginResponse
import dev.yidafu.aqua.api.service.DeliveryAuthService
import dev.yidafu.aqua.common.annotation.AdminService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

/**
 * 管理端配送员认证 GraphQL 解析器
 * 提供配送员登录和手机号绑定的 GraphQL 变更功能
 */
@AdminService
@Controller
class DeliveryAuthMutationResolver(
  private val deliveryAuthService: DeliveryAuthService,
) {
  @MutationMapping
  fun deliveryLogin(
    @Argument @Valid input: dev.yidafu.aqua.common.graphql.generated.DeliveryLoginInput,
  ): DeliveryLoginResponse = deliveryAuthService.login(DeliveryLoginRequest(input.code))

  @MutationMapping
  fun bindDeliveryPhone(
    @Argument @Valid input: dev.yidafu.aqua.common.graphql.generated.BindDeliveryPhoneInput,
  ): DeliveryLoginResponse = deliveryAuthService.bindPhone(input.openId, input.phoneNumber)
}
