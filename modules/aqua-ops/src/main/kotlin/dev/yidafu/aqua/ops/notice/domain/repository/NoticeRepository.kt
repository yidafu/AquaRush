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

package dev.yidafu.aqua.ops.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.NoticeModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * 公告仓库
 * 注意:@SoftDelete 自动过滤已删行,派生查询/JPQL 均不含软删数据。
 * 原手写 @Query 方法已迁移至 QueryDSL,见 [NoticeRepositoryCustom]/[NoticeRepositoryImpl]。
 */
@Repository
interface NoticeRepository :
  JpaRepository<NoticeModel, Long>,
  JpaSpecificationExecutor<NoticeModel>,
  NoticeRepositoryCustom
