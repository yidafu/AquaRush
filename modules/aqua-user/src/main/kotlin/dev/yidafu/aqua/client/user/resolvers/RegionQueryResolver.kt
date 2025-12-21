package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.model.RegionHierarchyModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class RegionQueryResolver(
    private val regionRepository: RegionRepository,
    @Value($$"${aqua.region.default-district-code:}") private val defaultDistrictCode: String?
) : BaseGraphQLResolver() {

  @QueryMapping
  fun regions(
      @Argument level: Int?,
      @Argument parentCode: String?,
      @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): List<RegionModel> {
    // 记录操作日志
    logOperation(userPrincipal, "regions", mapOf<String, Any>(
      "level" to (level ?: 0),
      "parentCode" to (parentCode ?: "")
    ))

    // 地区查询通常不需要严格的权限控制，但保留日志记录
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

  @QueryMapping
  fun region(
    @Argument code: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): RegionModel? {
    // 记录操作日志
    logOperation(userPrincipal, "region", mapOf("code" to code))

    return regionRepository.findByCode(code)
  }

  @QueryMapping
  fun defaultRegionHierarchy(
    @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): RegionHierarchyModel? {
    // 记录操作日志
    logOperation(userPrincipal, "defaultRegionHierarchy", mapOf<String, Any>(
      "defaultDistrictCode" to (defaultDistrictCode ?: "")
    ))

    return defaultDistrictCode?.let { buildRegionHierarchy(it) }
  }

  private fun buildRegionHierarchy(districtCode: String): RegionHierarchyModel? {
    try {
      // 1. 获取区县 (level=3)
      val district = regionRepository.findByCode(districtCode) ?: return null

      // 2. 获取城市 (level=2) - 区县的父级
      val city = district.parentCode?.let { regionRepository.findByCode(it) } ?: return null

      // 3. 获取省份 (level=1) - 城市的父级
      val province = city.parentCode?.let { regionRepository.findByCode(it) } ?: return null

      // 4. 获取所有省份（用于选择）
      val allProvinces = regionRepository.findByLevelOrderByCode(1)

      // 5. 获取该省份下的所有城市（用于选择）
      val citiesInProvince = province.code?.let { regionRepository.findByParentCodeOrderByCode(it) } ?: emptyList()

      // 6. 获取该城市下的所有区县（用于选择）
      val districtsInCity = city.code?.let { regionRepository.findByParentCodeOrderByCode(it) } ?: emptyList()

      return RegionHierarchyModel(
        province = province,
        city = city,
        district = district,
        provinces = allProvinces,
        cities = citiesInProvince,
        districts = districtsInCity
      )
    } catch (e: Exception) {
      // 记录错误但不抛出异常，返回 null 让前端处理
      println("Error building region hierarchy for district code $districtCode: ${e.message}")
      return null
    }
  }
}
