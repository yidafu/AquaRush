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

package dev.yidafu.aqua.user.mapper

import dev.yidafu.aqua.api.dto.UserStatus as DomainUserStatus
import dev.yidafu.aqua.common.graphql.generated.UserStatus as GraphQLUserStatus

object UserStatusMapper {
  fun toDomainStatus(graphqlStatus: GraphQLUserStatus): DomainUserStatus {
    return when (graphqlStatus) {
      GraphQLUserStatus.ACTIVE -> DomainUserStatus.ACTIVE
      GraphQLUserStatus.INACTIVE -> DomainUserStatus.INACTIVE
      GraphQLUserStatus.SUSPENDED -> DomainUserStatus.SUSPENDED
      GraphQLUserStatus.DELETED -> DomainUserStatus.DELETED
    }
  }

  fun toGraphQLStatus(domainStatus: DomainUserStatus): GraphQLUserStatus {
    return when (domainStatus) {
      DomainUserStatus.ACTIVE -> GraphQLUserStatus.ACTIVE
      DomainUserStatus.INACTIVE -> GraphQLUserStatus.INACTIVE
      DomainUserStatus.SUSPENDED -> GraphQLUserStatus.SUSPENDED
      DomainUserStatus.DELETED -> GraphQLUserStatus.DELETED
    }
  }
}