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

package dev.yidafu.aqua.logging.config

import dev.yidafu.aqua.logging.formatter.BusinessLogFormatter
import dev.yidafu.aqua.logging.formatter.StructuredLogFormatter
import dev.yidafu.aqua.logging.interceptor.CorrelationFilter
import dev.yidafu.aqua.logging.interceptor.LoggingInterceptor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 日志模块自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties::class)
@Configuration(proxyBeanMethods = false)
class LoggingAutoConfiguration {
  @Bean
  fun structuredLogFormatter(): StructuredLogFormatter = StructuredLogFormatter.instance

  @Bean
  fun businessLogFormatter(): BusinessLogFormatter = BusinessLogFormatter.instance

  @Bean
  fun correlationFilter(): FilterRegistrationBean<CorrelationFilter> {
    val registrationBean = FilterRegistrationBean<CorrelationFilter>()
    registrationBean.setFilter(CorrelationFilter())
    registrationBean.order = 1
    registrationBean.urlPatterns = listOf("/*")
    return registrationBean
  }

  @Bean
  fun loggingInterceptorConfigurer(): WebMvcConfigurer =
    object : WebMvcConfigurer {
      override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(LoggingInterceptor())
      }
    }
}
