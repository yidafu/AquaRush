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
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

/**
 * Base repository implementation for entities that support soft deletion.
 *
 * @param T the entity type
 * @param ID the entity id type
 */
open class SoftDeleteRepositoryImpl<T : SoftDeletable, ID : Any>(
    private val entityInformation: JpaEntityInformation<T, *>,
    @PersistenceContext
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), SoftDeleteRepository<T, ID> {

    private val entityClass: Class<T> = entityInformation.javaType

    override fun deleteByIdSoft(id: ID) {
        val entity = entityManager.find(entityClass, id)
        if (entity != null) {
            deleteSoft(entity)
        }
    }

    override fun deleteSoft(entity: T) {
        // Set deletedAt to current time
        entity.deletedAt = LocalDateTime.now()
        // Note: deletedBy should be set by the caller before calling this method
        entityManager.merge(entity)
    }

    override fun restore(id: ID) {
        val entity = findByIdIncludingDeleted(id)
        if (entity.isPresent) {
            val entityToRestore = entity.get()
            // Clear audit information
            entityToRestore.deletedAt = null
            entityToRestore.deletedBy = null
            entityManager.merge(entityToRestore)
        }
    }

    override fun findAllIncludingDeleted(): List<T> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(entityClass)
        val root = query.from(entityClass)
        return entityManager.createQuery(query.select(root)).resultList
    }

    override fun findByIdIncludingDeleted(id: ID): Optional<T> {
        return Optional.ofNullable(entityManager.find(entityClass, id))
    }

    override fun findAllDeleted(): List<T> {
        // With Hibernate @SoftDelete, we need to use native query or adjust criteria
        // to bypass the soft delete filter
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(entityClass)
        val root = query.from(entityClass)
        // Filter for entities where deletedAt is not null (i.e., soft deleted)
        query.where(cb.isNotNull(root.get<LocalDateTime>("deletedAt")))
        return entityManager.createQuery(query).resultList
    }

    override fun findAll(): List<T> {
        // This will be filtered by @Where annotation on the entity
        return super.findAll()
    }

    override fun findById(id: ID): Optional<T> {
        // This will be filtered by @Where annotation on the entity
        return super.findById(id)
    }
}
