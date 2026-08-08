/**
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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.graphql.generated.Address
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.BatchImportAddressesResult
import dev.yidafu.aqua.user.mapper.AddressInputMapper
import dev.yidafu.aqua.user.mapper.AddressMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AddressResolver(
  private val addressService: AddressService,
) {
  private val logger = LoggerFactory.getLogger(AddressResolver::class.java)

  @QueryMapping
  fun userAddresses(
    @Argument userId: Long,
  ): List<Address> {
    val list = addressService.findByUserId(userId)
    return AddressMapper.mapList(list)
  }

  // Admin-only queries

  /**
   * 获取所有地址 (管理员)
   */
  @QueryMapping
  fun addresses(): List<Address> {
    val allAddresses = addressService.findAllAddresses()
    return AddressMapper.mapList(allAddresses)
  }

  /**
   * 搜索所有地址 (管理员)
   */
  @QueryMapping
  fun searchAllAddresses(
    @Argument keyword: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 10,
  ): List<Address> {
    val pageable =
      org.springframework.data.domain.PageRequest
        .of(page, size)
    val addresses = addressService.searchAllAddresses(keyword, pageable)
    return AddressMapper.mapList(addresses.content)
  }

  // Admin-only mutations

  /**
   * 管理员创建地址 (userId 可以为 null)
   */
  @MutationMapping
  fun createAdminAddress(
    @Argument @Valid input: AddressInput,
  ): Address {
    try {
      val address = AddressInputMapper.map(input)
      // userId 保持为 null，由 AddressInput 中的值决定
      val savedAddress = addressService.save(address)
      logger.info("Admin created address with ID: ${savedAddress.id}")
      return AddressMapper.map(savedAddress)
    } catch (e: Exception) {
      logger.error("Failed to create admin address", e)
      throw RuntimeException("创建地址失败: ${e.message}")
    }
  }

  /**
   * 批量导入地址 (管理员)
   */
  @MutationMapping
  fun batchImportAddresses(
    @Argument input: List<AddressInput>,
  ): BatchImportAddressesResult {
    try {
      val totalCount = input.size
      var successCount = 0
      var failureCount = 0

      val addresses =
        input
          .mapIndexed { index, addressInput ->
            try {
              val address = AddressInputMapper.map(addressInput)
              // userId 保持为 null
              successCount++
              address
            } catch (e: Exception) {
              failureCount++
              logger.warn("Failed to parse address at index $index: ${e.message}")
              null
            }
          }.filterNotNull()

      // 批量保存
      if (addresses.isNotEmpty()) {
        addressService.saveAll(addresses)
      }

      logger.info("Batch import completed: $successCount success, $failureCount failed out of $totalCount")

      return BatchImportAddressesResult(
        successCount = successCount,
        failureCount = failureCount,
        totalCount = totalCount,
      )
    } catch (e: Exception) {
      logger.error("Failed to batch import addresses", e)
      throw RuntimeException("批量导入地址失败: ${e.message}")
    }
  }

  /**
   * 管理员删除地址 (不校验 userId)
   */
  @MutationMapping
  fun deleteAdminAddress(
    @Argument id: Long,
  ): Boolean = addressService.deleteAddressById(id)
}
