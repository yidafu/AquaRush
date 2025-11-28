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

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.api.dto.AddressDTO
import dev.yidafu.aqua.user.domain.model.Address
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
object AddressMapper : ObjectMappie<Address, AddressDTO>() {
  private val objectMapper = jacksonObjectMapper()

  override fun map(from: Address): AddressDTO {
    // 处理 coordinates 的 JSON 转换
    val coordinates = from.coordinates?.let { json ->
      try {
        objectMapper.readValue(json, dev.yidafu.aqua.api.dto.Coordinates::class.java)
      } catch (e: Exception) {
        null
      }
    }

    return AddressDTO(
      id = from.id,
      userId = from.userId,
      receiverName = from.receiverName,
      phone = from.phone,
      province = from.province,
      city = from.city,
      district = from.district,
      detailAddress = from.detailAddress,
      postalCode = from.postalCode,
      coordinates = coordinates,
      isDefault = from.isDefault,
    )
  }
}
