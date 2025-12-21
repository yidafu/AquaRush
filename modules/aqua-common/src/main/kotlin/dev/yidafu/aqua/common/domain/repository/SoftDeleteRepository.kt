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

package dev.yidafu.aqua.common.domain.repository

import dev.yidafu.aqua.common.domain.model.SoftDeletable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*

/**
 * Base repository interface for entities that support soft deletion.
 *
 * @param T the entity type
 * @param ID the entity id type
 */
@NoRepositoryBean
interface SoftDeleteRepository<T : SoftDeletable, ID : Any> : JpaRepository<T, ID> {
    /**
     * Find all entities including deleted ones.
     *
     * @return list of all entities
     */
    fun findAllIncludingDeleted(): List<T>

    /**
     * Find entity by id including deleted ones.
     *
     * @param id the entity id
     * @return optional of the entity
     */
    fun findByIdIncludingDeleted(id: ID): Optional<T>

    /**
     * Soft delete entity by id.
     *
     * @param id the entity id
     */
    fun deleteByIdSoft(id: ID)

    /**
     * Soft delete entity.
     *
     * @param entity the entity to delete
     */
    fun deleteSoft(entity: T)

    /**
     * Restore deleted entity by id.
     *
     * @param id the entity id
     */
    fun restore(id: ID)

    /**
     * Find all deleted entities.
     *
     * @return list of deleted entities
     */
    fun findAllDeleted(): List<T>
}
