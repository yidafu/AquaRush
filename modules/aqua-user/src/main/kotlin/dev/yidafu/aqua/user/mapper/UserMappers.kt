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
import dev.yidafu.aqua.api.dto.NotificationSettingsDTO
import dev.yidafu.aqua.api.dto.UpdateAddressRequest
import dev.yidafu.aqua.api.dto.UserDTO
import dev.yidafu.aqua.common.graphql.generated.Admin
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.Region
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.user.domain.model.AddressModel
import dev.yidafu.aqua.user.domain.model.AdminModel
import dev.yidafu.aqua.user.domain.model.NotificationSettingsModel
import dev.yidafu.aqua.user.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.model.UserModel
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

// ============================================================================
// Domain Model → GraphQL Mappers
// ============================================================================

/**
 * Mapper for converting UserModel domain entity to GraphQL User type
 */
object UserMapper : ObjectMappie<UserModel, User>() {
    override fun map(from: UserModel) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
    }
}

/**
 * Mapper for converting Admin domain entity to GraphQL Admin type
 */
object AdminMapper : ObjectMappie<AdminModel, Admin>() {
    override fun map(from: AdminModel) = mapping {
        to::role fromExpression { admin -> admin.role.name }
    }
}

/**
 * Mapper for converting AddressModel domain entity to GraphQL Address type
 */
object AddressMapper : ObjectMappie<AddressModel, Address>() {
    override fun map(from: AddressModel) = mapping {
        to::longitude fromValue from.longitude?.toFloat()
        to::latitude fromValue from.latitude?.toFloat()
        to::detailAddress fromValue from.detailAddress
        // Most fields map automatically by name
        // Note: Mappie automatically handles Double to Float conversion for nullable fields
    }
}

/**
 * Mapper for converting Region domain entity to GraphQL Region type
 */
object RegionMapper : ObjectMappie<RegionModel, Region>() {
    override fun map(from: RegionModel) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
    }
}

// ============================================================================
// DTO → Domain Model Mappers
// ============================================================================

/**
 * Mapper for converting NotificationSettingsDTO to NotificationSettingsModel
 */
object NotificationSettingsDTOToModelMapper : ObjectMappie<NotificationSettingsDTO, NotificationSettingsModel>() {
    override fun map(from: NotificationSettingsDTO) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
    }
}

/**
 * Mapper for converting NotificationSettingsModel to NotificationSettingsDTO
 */
object NotificationSettingsModelToDTOMapper : ObjectMappie<NotificationSettingsModel, NotificationSettingsDTO>() {
    override fun map(from: NotificationSettingsModel) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
    }
}

/**
 * Mapper for converting UserModel to UserDTO
 */
object UserModelToDTOMapper : ObjectMappie<UserModel, UserDTO>() {
    override fun map(from: UserModel) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
      to::addresses fromValue emptyList()
      to::notificationSettings fromValue NotificationSettingsDTO()
    }
}

/**
 * Mapper for converting UserDTO to UserModel
 */
object UserDTOToModelMapper : ObjectMappie<UserDTO, UserModel>() {
    override fun map(from: UserDTO) = mapping {
        // Most fields map automatically by name
        // No custom mapping needed as field names match
    }
}

// ============================================================================
// Request → Domain Model Mappers (with additional parameters)
// ============================================================================

/**
 * Mapper for converting CreateAddressRequest with userId to AddressModel domain entity
 */
@Component
object CreateAddressRequestWithUserIdMapper {
    fun map(from: CreateAddressRequest, userId: Long): AddressModel {
        return AddressModel(
            id = -1L,
            userId = userId,
            province = from.province,
            provinceCode = null,
            city = from.city,
            cityCode = null,
            district = from.district,
            districtCode = null,
            detailAddress = from.detailedAddress,
            postalCode = from.postalCode,
            longitude = 0.0, // from.longitude?.toDouble(),
            latitude = 0.0, // from.latitude?.toDouble(),
            isDefault = from.isDefault
        )
    }
}

/**
 * Mapper for converting UpdateAddressRequest with id and userId to AddressModel domain entity
 */
@Component
object UpdateAddressRequestWithIdsMapper {
    fun map(from: UpdateAddressRequest, id: Long, userId: Long): AddressModel {
        return AddressModel(
            id = id,
            userId = userId,
            province = from.province,
            provinceCode = null,
            city = from.city,
            cityCode = null,
            district = from.district,
            districtCode = null,
            detailAddress = from.detailedAddress,
            postalCode = from.postalCode,
            longitude = 0.0, // from.longitude?.toDouble(),
            latitude = 0.0, // from.latitude?.toDouble(),
            isDefault = from.isDefault
        )
    }
}

