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

package dev.yidafu.aqua.client.config

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
//import tools.jackson.datatype.jsr310.JavaTimeModule
import tools.jackson.module.kotlin.KotlinModule
//import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Spring Boot 4.0 Jackson 3.x 兼容配置
 *
 * 主要变更：
 * 1. 包名从 com.fasterxml 改为 tools.jackson
 * 2. ObjectMapper 创建方式改为 JsonMapper.builder()
 * 3. 异常处理简化（从受检异常改为运行时异常）
 * 4. 配置属性使用大写枚举常量
 */
@Configuration
class JacksonConfig {

    /**
     * 主要的 ObjectMapper Bean，使用 Jackson 3.x 的 JsonMapper
     */
    @Bean("primaryObjectMapper")
    fun primaryObjectMapper(): ObjectMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
//            .addModule(JavaTimeModule())
            // 序列化配置
//            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            // 反序列化配置
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .build()
    }

    /**
     * Jackson2ObjectMapperBuilder 自定义器，保持与 Spring Boot 自动配置的兼容性
     */
//    @Bean
//    fun jacksonObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
//        return Jackson2ObjectMapperBuilderCustomizer { builder ->
//            // 配置 Kotlin 模块
//            builder.modulesToInstall(KotlinModule.Builder().build(), JavaTimeModule())
//
//            // Spring Boot 4.0 配置属性兼容
//            builder.featuresToDisable(
//                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
//                SerializationFeature.FAIL_ON_EMPTY_BEANS,
//                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
//                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
//                DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES
//            )
//        }
//    }

    /**
     * 用于 API 响应的专用 ObjectMapper
     */
    @Bean("apiObjectMapper")
    fun apiObjectMapper(): ObjectMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
//            .addModule(JavaTimeModule())
            // API 专用配置：禁用空值序列化，启用缩进输出
//            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    }

    /**
     * 用于日志记录的专用 ObjectMapper
     */
    @Bean("loggingObjectMapper")
    fun loggingObjectMapper(): ObjectMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
//            .addModule(JavaTimeModule())
            // 日志专用配置：紧凑格式，禁用所有美观化
//            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    }
}
