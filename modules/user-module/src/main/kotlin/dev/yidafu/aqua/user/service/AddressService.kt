package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.model.Address
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AddressService(
    private val addressRepository: AddressRepository
) {
    fun findByUserId(userId: UUID): List<Address> {
        return addressRepository.findByUserId(userId)
    }

    fun findById(addressId: UUID): Address? {
        return addressRepository.findById(addressId).orElse(null)
    }

    fun findDefaultAddress(userId: UUID): Address? {
        return addressRepository.findByUserIdAndIsDefault(userId, true)
    }

    @Transactional
    fun createAddress(
        userId: UUID,
        receiverName: String,
        phone: String,
        province: String,
        city: String,
        district: String,
        detailAddress: String,
        isDefault: Boolean = false
    ): Address {
        // 如果设置为默认地址，需要先清除该用户的其他默认地址
        if (isDefault) {
            addressRepository.clearDefaultByUserId(userId)
        }

        val address = Address(
            userId = userId,
            receiverName = receiverName,
            phone = phone,
            province = province,
            city = city,
            district = district,
            detailAddress = detailAddress,
            isDefault = isDefault
        )
        return addressRepository.save(address)
    }

    @Transactional
    fun updateAddress(
        addressId: UUID,
        userId: UUID,
        receiverName: String?,
        phone: String?,
        province: String?,
        city: String?,
        district: String?,
        detailAddress: String?,
        isDefault: Boolean?
    ): Address {
        val address = addressRepository.findById(addressId)
            .orElseThrow { IllegalArgumentException("Address not found: $addressId") }

        if (address.userId != userId) {
            throw IllegalArgumentException("Address does not belong to user")
        }

        // 如果要设置为默认地址，先清除其他默认地址
        if (isDefault == true && !address.isDefault) {
            addressRepository.clearDefaultByUserId(userId)
        }

        receiverName?.let { address.receiverName = it }
        phone?.let { address.phone = it }
        province?.let { address.province = it }
        city?.let { address.city = it }
        district?.let { address.district = it }
        detailAddress?.let { address.detailAddress = it }
        isDefault?.let { address.isDefault = it }

        return addressRepository.save(address)
    }

    @Transactional
    fun deleteAddress(addressId: UUID, userId: UUID): Boolean {
        val count = addressRepository.deleteByIdAndUserId(addressId, userId)
        return count > 0
    }

    @Transactional
    fun setDefaultAddress(addressId: UUID, userId: UUID): Address {
        val address = addressRepository.findById(addressId)
            .orElseThrow { IllegalArgumentException("Address not found: $addressId") }

        if (address.userId != userId) {
            throw IllegalArgumentException("Address does not belong to user")
        }

        if (!address.isDefault) {
            addressRepository.clearDefaultByUserId(userId)
            address.isDefault = true
            return addressRepository.save(address)
        }

        return address
    }
}
