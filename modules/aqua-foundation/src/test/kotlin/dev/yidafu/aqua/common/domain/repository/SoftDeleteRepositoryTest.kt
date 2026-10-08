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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.common.domain.repository

import dev.yidafu.aqua.common.domain.model.SoftDeletable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import java.time.LocalDateTime
import java.util.*

class SoftDeleteRepositoryTest :
  FunSpec({
    data class TestEntity(
      override var deletedAt: LocalDateTime? = null,
      override var deletedBy: Long? = null,
    ) : SoftDeletable
    var repository: SoftDeleteRepositoryImpl<TestEntity, Long> = mockk()
    var entityManager: EntityManager = mockk()
    var entityInformation: JpaEntityInformation<TestEntity, *> = mockk()

    beforeTest {
      entityManager = mockk(relaxed = true)
      entityInformation = mockk()
      every { entityInformation.javaType } returns TestEntity::class.java

      repository = SoftDeleteRepositoryImpl(entityInformation, entityManager)
    }
    test("test deleteSoft should mark entity as deleted") {
      // Given
      val entity = TestEntity()

      // When
      repository.deleteSoft(entity)

      // Then
      entity.deletedAt.shouldNotBeNull()
      entity.deletedAt.shouldNotBeNull()
      verify { entityManager.merge(entity) }
    }
    test("test restore should mark entity as not deleted") {
      // Given
      val entity =
        TestEntity().apply {
          deletedAt = LocalDateTime.now()
          deletedAt = LocalDateTime.now()
        }
      val optional = Optional.of(entity)
      every { entityManager.find(TestEntity::class.java, 1L) } returns entity

      // When
      repository.restore(1L)

      // Then
      entity.deletedAt.shouldBeNull()
      entity.deletedAt.shouldBeNull()
      verify { entityManager.merge(entity) }
    }
  })
