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

package dev.yidafu.aqua.user.mapper

import dev.yidafu.aqua.api.dto.CreateAddressRequest
import dev.yidafu.aqua.api.dto.UpdateAddressRequest
import dev.yidafu.aqua.user.domain.model.Address
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

/**
 * CreateAddressRequest 到 Address 的映射器
 */
@Component
object CreateAddressRequestMapper : ObjectMappie<CreateAddressRequest, Address>()

/**
 * UpdateAddressRequest 到 Address 的映射器
 */
@Component
object UpdateAddressRequestMapper : ObjectMappie<UpdateAddressRequest, Address>()
