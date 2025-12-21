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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.exception.AquaException
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@Transactional
class RegionService(
    private val regionRepository: RegionRepository
) {

  /**
   * 创建新地区
   */
  fun createRegion(input: CreateRegionInput): RegionModel {
    // 验证代码唯一性
    if (regionRepository.existsByCode(input.code)) {
      throw AquaException("地区代码已存在: ${input.code}")
    }

    // 验证层级
    if (input.level < 1 || input.level > 3) {
      throw AquaException("地区层级必须在 1-3 之间")
    }

    // 验证父级地区
    val parentCode = if (input.level == 1) {
      "0" // 根级地区的父代码为 "0"
    } else {
      input.parentCode ?: throw AquaException("非根级地区必须指定父级代码")
    }

    // 验证父级地区是否存在（如果指定了父级代码）
    if (parentCode != "0") {
      val parentRegion = regionRepository.findByCode(parentCode)
        ?: throw AquaException("父级地区不存在: $parentCode")

      // 验证层级关系
      if (parentRegion.level != input.level - 1) {
        throw AquaException("父级地区层级不正确")
      }
    }

    // 验证同一父级下名称和代码的唯一性
    if (regionRepository.existsByNameAndLevelAndParentCode(input.name, input.level, parentCode)) {
      throw AquaException("在同一父级下已存在同名地区: ${input.name}")
    }

    // 创建地区
    val region = RegionModel(
      name = input.name,
      code = input.code,
      parentCode = parentCode,
      level = input.level
    )

    return regionRepository.save(region)
  }

  /**
   * 更新地区信息
   */
  @CacheEvict(value = ["regions"], allEntries = true)
  fun updateRegion(code: String, input: UpdateRegionInput): RegionModel {
    val region = regionRepository.findByCode(code)
      ?: throw AquaException("地区不存在: $code")

    // 更新名称
    input.name?.let { newName ->
      if (newName != region.name) {
        // 验证同一父级下名称的唯一性
        if (regionRepository.existsByNameAndLevelAndParentCode(newName, region.level, region.parentCode!!)) {
          throw AquaException("在同一父级下已存在同名地区: $newName")
        }
        // 由于 Region 是不可变的，需要创建新实例
        return createUpdatedRegion(region, name = newName)
      }
    }

    // 更新父级地区
    input.parentCode?.let { newParentCode ->
      if (newParentCode != region.parentCode) {
        // 验证新的父级地区
        val newParent = if (region.level == 1) {
          if (newParentCode != "0") {
            throw AquaException("根级地区的父代码必须为 '0'")
          }
          null
        } else {
          regionRepository.findByCode(newParentCode)
            ?: throw AquaException("父级地区不存在: $newParentCode")
        }

        // 验证层级关系
        newParent?.let { parent ->
          if (parent.level != region.level - 1) {
            throw AquaException("父级地区层级不正确")
          }
        }

        // 验证是否有子地区（如果有子地区，不能随意更改父级）
        val hasChildren = regionRepository.findByParentCodeOrderByCode(code).isNotEmpty()
        if (hasChildren && region.level > 1) {
          throw AquaException("存在子地区的地区不能更改父级关系")
        }

        // 验证新父级下的名称唯一性
        if (input.name != null) {
          if (regionRepository.existsByNameAndLevelAndParentCode(input.name, region.level, newParentCode)) {
            throw AquaException("在新的父级下已存在同名地区")
          }
        } else if (regionRepository.existsByNameAndLevelAndParentCode(region.name, region.level, newParentCode)) {
          throw AquaException("在新的父级下已存在同名地区")
        }

        return createUpdatedRegion(region, parentCode = newParentCode, name = input.name)
      }
    }

    // 如果没有实际更改，返回原地区
    return region
  }

  /**
   * 删除地区
   */
  @CacheEvict(value = ["regions"], allEntries = true)
  fun deleteRegion(code: String): Boolean {
    val region = regionRepository.findByCode(code)
      ?: throw AquaException("地区不存在: $code")

    // 检查是否有子地区
    val hasChildren = regionRepository.findByParentCodeOrderByCode(code).isNotEmpty()
    if (hasChildren) {
      throw AquaException("存在子地区的地区不能删除")
    }

    // 检查是否被地址引用（这里假设有地址引用检查）
    // TODO: 添加地址引用检查逻辑
    // if (addressRepository.existsByRegionCode(code)) {
    //   throw AquaException("该地区被地址引用，不能删除")
    // }

    regionRepository.delete(region)
    return true
  }

  /**
   * 根据层级和父代码获取地区列表
   */
  @Cacheable(value = ["regions"], key = "#level.toString() + '_' + (#parentCode ?: 'root')")
  fun getRegions(level: Int?, parentCode: String?): List<RegionModel> {
    return when {
      level != null && parentCode != null -> {
        regionRepository.findByParentCodeAndLevel(parentCode, level)
      }
      level != null -> {
        if (level == 1) {
          regionRepository.findRootRegions(level)
        } else {
          regionRepository.findByLevel(level)
        }
      }
      else -> {
        regionRepository.findAll()
      }
    }
  }

  /**
   * 根据代码获取地区
   */
  @Cacheable(value = ["regions"], key = "#code")
  fun getRegionByCode(code: String): RegionModel? {
    return regionRepository.findByCode(code)
  }

  /**
   * 搜索地区
   */
  fun searchRegions(keyword: String, level: Int?): List<RegionModel> {
    return if (level != null) {
      regionRepository.findByNameContainingAndLevelOrderByCode(keyword, level)
    } else {
      regionRepository.findByNameContainingOrderByCode(keyword)
    }
  }

  /**
   * 创建更新后的地区实例（由于 Region 是不可变的）
   */
  private fun createUpdatedRegion(
    original: RegionModel,
    name: String? = null,
    parentCode: String? = null
  ): RegionModel {
    return RegionModel(
      id = original.id,
      name = name ?: original.name,
      code = original.code,
      parentCode = parentCode ?: original.parentCode,
      level = original.level,
      createdAt = original.createdAt,
      updatedAt = java.time.LocalDateTime.now()
    )
  }
}

// Input types
data class CreateRegionInput(
  val name: String,
  val code: String,
  val level: Int,
  val parentCode: String?
)

data class UpdateRegionInput(
  val name: String?,
  val parentCode: String?
)
