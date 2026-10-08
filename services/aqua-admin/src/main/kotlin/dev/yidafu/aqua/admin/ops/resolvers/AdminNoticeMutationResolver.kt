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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.NoticeMutationService
import dev.yidafu.aqua.common.graphql.generated.CreateNoticeInput
import dev.yidafu.aqua.common.graphql.generated.Notice
import dev.yidafu.aqua.common.graphql.generated.UpdateNoticeInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.notice.mapper.CreateNoticeInputMapper
import dev.yidafu.aqua.ops.notice.mapper.NoticeMapper
import dev.yidafu.aqua.ops.notice.mapper.UpdateNoticeInputMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 公告变更 Resolver(管理后台,PERMISSION_OPS_CONFIG_WRITE)
 * 生命周期(PRD 12-operations-config.md §F-OPS-002):新建 → 发布 → 置顶 → 停用 → 删除;
 * 操作人取自登录主体,由服务落审计日志(R-OPS-01)。
 */
@Controller
class AdminNoticeMutationResolver(
  private val noticeMutationService: NoticeMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminNoticeMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun createNotice(
    @Argument @Valid input: CreateNoticeInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Notice {
    logger.info("Admin {} creating notice: {}", principal.username, input.title)
    val notice =
      noticeMutationService.createNotice(
        adminId = principal.id,
        adminName = principal.username,
        request = CreateNoticeInputMapper.map(input),
      )
    return NoticeMapper.map(notice)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun updateNotice(
    @Argument id: Long,
    @Argument @Valid input: UpdateNoticeInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Notice {
    logger.info("Admin {} updating notice {}", principal.username, id)
    val notice =
      noticeMutationService.updateNotice(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        request = UpdateNoticeInputMapper.map(input),
      )
    return NoticeMapper.map(notice)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun publishNotice(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Notice {
    logger.info("Admin {} publishing notice {}", principal.username, id)
    return NoticeMapper.map(noticeMutationService.publishNotice(principal.id, principal.username, id))
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun stopNotice(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Notice {
    logger.info("Admin {} stopping notice {}", principal.username, id)
    return NoticeMapper.map(noticeMutationService.stopNotice(principal.id, principal.username, id))
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun setNoticePinned(
    @Argument id: Long,
    @Argument pinned: Boolean,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Notice {
    logger.info("Admin {} setting notice {} pinned={}", principal.username, id, pinned)
    return NoticeMapper.map(noticeMutationService.setNoticePinned(principal.id, principal.username, id, pinned))
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun deleteNotice(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} deleting notice {}", principal.username, id)
    return noticeMutationService.deleteNotice(principal.id, principal.username, id)
  }
}
