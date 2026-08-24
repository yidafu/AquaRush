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

package dev.yidafu.aqua.marketing.promotion.domain.repository

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * 满减/折扣活动仓库(F-MKT-004)
 * 原手写 @Query 方法已迁移至 QueryDSL,见 [PromotionActivityRepositoryCustom]/[PromotionActivityRepositoryImpl]。
 */
@Repository
interface PromotionActivityRepository :
  JpaRepository<PromotionActivityModel, Long>,
  JpaSpecificationExecutor<PromotionActivityModel>,
  PromotionActivityRepositoryCustom
