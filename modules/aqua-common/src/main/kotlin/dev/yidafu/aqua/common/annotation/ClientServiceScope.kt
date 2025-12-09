/*
 * AquaRush Service Scope Annotation
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

package dev.yidafu.aqua.common.annotation

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

const val AQUA_MODE_CLIENT = "client"
const val AQUA_MODE_ADMIN = "admin"

class ClientServiceScope : Condition {
  override fun matches(
    context: ConditionContext,
    metadata: AnnotatedTypeMetadata,
  ): Boolean {
    return context.environment.getProperty("aqua.mode") == AQUA_MODE_CLIENT
  }
}

class AdminServiceScope : Condition {
  override fun matches(
    context: ConditionContext,
    metadata: AnnotatedTypeMetadata,
  ): Boolean {
    return context.environment.getProperty("aqua.mode") == AQUA_MODE_ADMIN
  }
}

/**
 * Conditional annotation for client service
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(ClientServiceScope::class)
annotation class ClientService

/**
 * Conditional annotation for admin service
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(AdminServiceScope::class)
annotation class AdminService
