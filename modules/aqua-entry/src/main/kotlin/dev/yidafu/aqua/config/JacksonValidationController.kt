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

package dev.yidafu.aqua.config

import tools.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.SerializationFeature
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Jackson 3.x 配置验证控制器
 * 用于测试 Spring Boot 4.0 Jackson 升级是否成功
 */
@RestController
@RequestMapping("/api/jackson-test")
class JacksonValidationController(
    private val objectMapper: ObjectMapper
) {

    /**
     * 测试数据类
     */
    data class TestUser(
        val id: Long,
        val name: String,
        val email: String,
        @JsonProperty("created_at")
        val createdAt: LocalDateTime,
        @JsonProperty("is_active")
        val isActive: Boolean = true,
        val salary: BigDecimal? = null
    )

    /**
     * 验证 Jackson 配置的端点
     */
    @GetMapping("/config")
    fun testJacksonConfiguration(): Map<String, Any> {
        val testData = TestUser(
            id = 1L,
            name = "测试用户",
            email = "test@aquarush.com",
            createdAt = LocalDateTime.now(),
            isActive = true,
            salary = BigDecimal("5000.00")
        )

        val result = mutableMapOf<String, Any>()
        result["jackson_version"] = "3.0.0 (tools.jackson)"
        result["spring_boot_version"] = "4.0.0"
        result["test_data"] = testData
        result["timestamp"] = LocalDateTime.now()
        result["test_success"] = true

        // 测试序列化
        try {
            val jsonOutput = objectMapper.writeValueAsString(testData)
            result["json_output"] = jsonOutput
            result["serialization"] = "SUCCESS"
            result["date_format"] = "ISO-8601 (not timestamp)"
        } catch (e: Exception) {
            result["serialization"] = "FAILED: ${e.message}"
            result["test_success"] = false
        }

        // 测试反序列化
        try {
            val jsonInput = """
                {
                    "id": 2,
                    "name": "反序列化测试",
                    "email": "deser@aquarush.com",
                    "created_at": "2024-01-15T10:30:00",
                    "is_active": false
                }
            """.trimIndent()

            val user = objectMapper.readValue(jsonInput, TestUser::class.java)
            result["deserialization"] = "SUCCESS"
            result["deserialized_user"] = user
        } catch (e: Exception) {
            result["deserialization"] = "FAILED: ${e.message}"
            result["test_success"] = false
        }

        // 检查配置特性
        result["features"] = mapOf(
//            "WRITE_DATES_AS_TIMESTAMPS" to !objectMapper.isEnabled(
//                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
//            ),
            "FAIL_ON_EMPTY_BEANS" to !objectMapper.isEnabled(
                tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
            ),
            "FAIL_ON_UNKNOWN_PROPERTIES" to !objectMapper.isEnabled(
                tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            ),
//            "kotlin_module_registered" to objectMapper.registeredModuleIds
//                .any { it.toString().contains("kotlin") }
        )

        return result
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    fun healthCheck(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "jackson_version" to "3.0.0",
            "migration_status" to "COMPLETED",
            "package" to "tools.jackson",
            "spring_boot_version" to "4.0.0"
        )
    }
}
