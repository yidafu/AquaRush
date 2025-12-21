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
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.Admin
import dev.yidafu.aqua.common.graphql.generated.Region
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.model.UserModel
import tech.mappie.api.ObjectMappie
import java.time.LocalDateTime

// ============================================================================
// Domain Model → GraphQL Mappers
// ============================================================================

/**
 * Mapper for converting UserModel domain entity to GraphQL User type
 * Handles monetary field conversion from cents (Long) to GraphQL cent-based fields
 */
object UserMapper : ObjectMappie<UserModel, User>(){
  override fun map(from: UserModel): User  = mapping {
    to::id fromValue (from.id ?: -1L)
    // Monetary fields are already in cents (Long) in UserModel, so direct mapping works
    // No conversion needed as both source and target use cent-based Long fields
    to::balanceCents fromValue from.balanceCents
    to::totalSpentCents fromValue from.totalSpentCents
  }
}


/**
 * Mapper for converting Admin domain entity to GraphQL Admin type
 */
object AdminMapper : ObjectMappie<AdminModel, Admin>() {
  override fun map(from: AdminModel) =
    mapping {
      to::role fromExpression { admin -> admin.role.name }
    }
}

/**
 * Mapper for converting AddressModel domain entity to GraphQL Address type
 */
object AddressMapper : ObjectMappie<AddressModel, Address>() {
  override fun map(from: AddressModel) =
    mapping {
      to::id fromValue (from.id ?: -1L)
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
  override fun map(from: RegionModel) =
    mapping {
      // Most fields map automatically by name
      // No custom mapping needed as field names match
    }
}

// ============================================================================
// Monetary Conversion Utility Functions
// ============================================================================

/**
 * Utility functions for monetary conversions between BigDecimal and Long (cents)
 * These functions ensure precise financial calculations without floating-point errors
 */

/**
 * Convert BigDecimal amount to cents (Long)
 * @param amount BigDecimal amount in currency units (e.g., 12.34 for ¥12.34)
 * @return amount in cents (e.g., 1234 for ¥12.34)
 */
fun toCents(amount: java.math.BigDecimal): Long {
  return (amount * java.math.BigDecimal(100)).toLong()
}

/**
 * Convert cents (Long) to BigDecimal amount
 * @param cents amount in cents (e.g., 1234)
 * @return BigDecimal amount in currency units (e.g., 12.34)
 */
fun fromCents(cents: Long): java.math.BigDecimal {
  return java.math.BigDecimal(cents).divide(java.math.BigDecimal(100))
}

// ============================================================================
// Request → Domain Model Mappers (with additional parameters)
// ============================================================================

/**
 * Mapper for converting CreateAddressRequest with userId to AddressModel domain entity
 */
object AddressInputMapper : ObjectMappie<AddressInput, AddressModel>() {
  override fun map(from: AddressInput) =
    mapping {
      to::id fromValue null
      to::userId fromValue 0L // Will be set after mapping
      to::createdAt fromValue LocalDateTime.now()
      to::updatedAt fromValue LocalDateTime.now()
      // Convert Float to Double for nullable fields
      to::longitude fromValue from.longitude?.toDouble()
      to::latitude fromValue from.latitude?.toDouble()
    }
}
/**
 * Mapper for converting UpdateAddressRequest with id and userId to AddressModel domain entity
 */
object AddressUpdateMapper : ObjectMappie<UpdateAddressInput, AddressModel>() {
  override fun map(from: UpdateAddressInput): AddressModel  = mapping {

    to::id fromValue null
    to::userId fromValue 0L // Will be set after mapping

    to::province fromValue (from.province ?: "")
    to::receiverName fromValue (from.receiverName ?: "")
    to::phone fromValue (from.phone ?: "")
    to::provinceCode fromValue (from.provinceCode ?: "")
    to::city fromValue (from.city ?: "")
    to::cityCode fromValue (from.cityCode ?: "")
    to::district fromValue (from.district ?: "")
    to::districtCode fromValue (from.districtCode ?: "")
    to::detailAddress fromValue (from.detailAddress ?: "")
    to::isDefault fromValue (from.isDefault ?: false)

    to::createdAt fromValue LocalDateTime.now()
    to::updatedAt fromValue LocalDateTime.now()
    // Convert Float to Double for nullable fields
    to::longitude fromValue from.longitude?.toDouble()
    to::latitude fromValue from.latitude?.toDouble()
  }
}

fun AddressModel.merge(inputInput: UpdateAddressInput) {
  inputInput.receiverName?.let { this.receiverName = it }
  inputInput.phone?.let { this.phone = it }
  inputInput.province?.let { this.province = it }
  inputInput.provinceCode?.let { this.provinceCode = it }
  inputInput.city?.let { this.city = it }
  inputInput.cityCode?.let { this.cityCode = it }
  inputInput.district?.let { this.district = it }
  inputInput.districtCode?.let { this.districtCode = it }
  inputInput.detailAddress?.let { this.detailAddress = it }
  inputInput.longitude?.let { this.longitude = it.toDouble() }
  inputInput.latitude?.let { this.latitude = it.toDouble() }
  inputInput.isDefault?.let { this.isDefault = it }
}
