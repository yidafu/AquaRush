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

package dev.yidafu.aqua.common.graphql

import dev.yidafu.aqua.common.graphql.scalars.BigDecimalScalar
import dev.yidafu.aqua.common.graphql.scalars.LocalDateTimeScalar
import dev.yidafu.aqua.common.graphql.scalars.LongScalar
import dev.yidafu.aqua.common.graphql.scalars.MapScalar
import dev.yidafu.aqua.common.graphql.scalars.MoneyScalar
import dev.yidafu.aqua.common.graphql.scalars.JsonObjectScalar
import dev.yidafu.aqua.common.graphql.scalars.JsonArrayScalar
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.stereotype.Component
import java.lang.Long
import kotlin.Any
import kotlin.String
import kotlin.collections.getValue
import kotlin.getValue



@Configuration
class GraphQLConfig {
  @Bean
  fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
    RuntimeWiringConfigurer { wiringBuilder ->
      wiringBuilder
        .scalar(BigDecimalScalar.GraphQL_TYPE)
        .scalar(LocalDateTimeScalar.GraphQL_TYPE)
        .scalar(LongScalar.GraphQL_TYPE)
        .scalar(MapScalar.GraphQL_TYPE)
        .scalar(JsonObjectScalar.GraphQL_TYPE)
        .scalar(JsonArrayScalar.GraphQL_TYPE)
        .scalar(MoneyScalar.GraphQL_TYPE)
    }
}
