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

package dev.yidafu.aqua.user.controller

import dev.yidafu.aqua.user.domain.model.Address
import dev.yidafu.aqua.user.service.AddressService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/addresses")
class AddressController(
  private val addressService: AddressService,
) {
  @GetMapping
  fun getMyAddresses(
    @RequestAttribute("userId") userId: Long,
  ): ResponseEntity<List<Address>> {
    val addresses = addressService.findByUserId(userId)
    return ResponseEntity.ok(addresses)
  }

  @GetMapping("/{id}")
  fun getAddressById(
    @PathVariable id: Long,
    @RequestAttribute("userId") userId: Long,
  ): ResponseEntity<Address> {
    val address =
      addressService.findById(id)
        ?: return ResponseEntity.notFound().build()

    if (address.userId != userId) {
      return ResponseEntity.status(403).build()
    }

    return ResponseEntity.ok(address)
  }

  @PostMapping
  fun createAddress(
    @RequestAttribute("userId") userId: Long,
    @RequestBody request: CreateAddressRequest,
  ): ResponseEntity<Address> {
    val address =
      addressService.createAddress(
        userId = userId,
        receiverName = request.receiverName,
        phone = request.phone,
        province = request.province,
        city = request.city,
        district = request.district,
        detailAddress = request.detailAddress,
        isDefault = request.isDefault ?: false,
      )
    return ResponseEntity.ok(address)
  }

  @PutMapping("/{id}")
  fun updateAddress(
    @PathVariable id: Long,
    @RequestAttribute("userId") userId: Long,
    @RequestBody request: UpdateAddressRequest,
  ): ResponseEntity<Address> {
    val address =
      addressService.updateAddress(
        addressId = id,
        userId = userId,
        receiverName = request.receiverName,
        phone = request.phone,
        province = request.province,
        city = request.city,
        district = request.district,
        detailAddress = request.detailAddress,
        isDefault = request.isDefault,
      )
    return ResponseEntity.ok(address)
  }

  @DeleteMapping("/{id}")
  fun deleteAddress(
    @PathVariable id: Long,
    @RequestAttribute("userId") userId: Long,
  ): ResponseEntity<Void> {
    val deleted = addressService.deleteAddress(id, userId)
    return if (deleted) {
      ResponseEntity.noContent().build()
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @PostMapping("/{id}/set-default")
  fun setDefaultAddress(
    @PathVariable id: Long,
    @RequestAttribute("userId") userId: Long,
  ): ResponseEntity<Address> {
    val address = addressService.setDefaultAddress(id, userId)
    return ResponseEntity.ok(address)
  }
}

data class CreateAddressRequest(
  val receiverName: String,
  val phone: String,
  val province: String,
  val city: String,
  val district: String,
  val detailAddress: String,
  val isDefault: Boolean? = false,
)

data class UpdateAddressRequest(
  val receiverName: String?,
  val phone: String?,
  val province: String?,
  val city: String?,
  val district: String?,
  val detailAddress: String?,
  val isDefault: Boolean?,
)
