package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.RegionVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class RegionQueryResolver(
  private val regionRepository: RegionRepository,
) : BaseGraphQLResolver() {
  @QueryMapping
  fun regions(
    @Argument level: Int?,
    @Argument parentCode: String?,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): List<RegionVo> {
    // 记录操作日志
    logOperation(
      userPrincipal,
      "regions",
      mapOf<String, Any>(
        "level" to (level ?: 0),
        "parentCode" to (parentCode ?: ""),
      ),
    )

    // 地区查询通常不需要严格的权限控制，但保留日志记录
    return try {
      val result: List<RegionVo> =
        when {
          level != null && parentCode != null -> {
            regionRepository.findByParentCodeAndLevel(parentCode, level) ?: emptyList()
          }

          level != null -> {
            if (level == 1) {
              regionRepository.findRootRegions(level) ?: emptyList()
            } else {
              regionRepository.findByLevel(level) ?: emptyList()
            }
          }

          else -> {
            // 无过滤条件时只返回省级(34 条),避免整表进内存(AGENTS.md 规则 10);
            // 客户端实际都按 level/parentCode 逐级查询
            regionRepository.findRootRegions(1)
          }
        }.map { it.toGraphQL() }
      result
    } catch (e: Exception) {
      println("Error fetching regions: ${e.message}")
      e.printStackTrace()
      emptyList()
    }
  }

  @QueryMapping
  fun region(
    @Argument code: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): RegionVo? {
    // 记录操作日志
    logOperation(userPrincipal, "region", mapOf("code" to code))

    return regionRepository.findByCode(code)?.toGraphQL()
  }
}
