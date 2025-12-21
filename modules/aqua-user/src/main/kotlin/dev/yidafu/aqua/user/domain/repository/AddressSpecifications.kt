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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.AddressModel
import org.springframework.data.jpa.domain.Specification

class AddressSpecifications {
    companion object {
        fun byUserId(userId: Long): Specification<AddressModel> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Long>("userId"), userId)
            }
        }

        fun byUserIdAndIsDefault(userId: Long, isDefault: Boolean): Specification<AddressModel> {
            return Specification { root, _, cb ->
                val userIdPredicate = cb.equal(root.get<Long>("userId"), userId)
                val isDefaultPredicate = cb.equal(root.get<Boolean>("isDefault"), isDefault)
                cb.and(userIdPredicate, isDefaultPredicate)
            }
        }
    }
}
