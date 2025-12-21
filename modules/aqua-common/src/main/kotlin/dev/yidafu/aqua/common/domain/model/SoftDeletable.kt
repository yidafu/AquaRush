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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.common.domain.model

import java.time.LocalDateTime

/**
 * Interface for entities that support soft deletion.
 *
 * All entities implementing this interface must provide these two fields:
 * - deletedAt: Timestamp when the entity was soft deleted (nullable)
 * - deletedBy: ID of the user who performed the soft deletion (nullable)
 *
 * Note: The isDeleted field is managed by Hibernate's @SoftDelete annotation
 * and should not be explicitly defined in entity classes.
 */
interface SoftDeletable {
    var deletedAt: LocalDateTime?
    var deletedBy: Long?
}
