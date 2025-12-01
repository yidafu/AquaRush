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

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class SimpleWebMvcConfigurer : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    // override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    //   // Add default resource handlers for static resources
    //   registry.addResourceHandler("/static/**")
    //     .addResourceLocations("classpath:/static/")

    //   // Add default resource handlers for webjars
    //   registry.addResourceHandler("/webjars/**")
    //     .addResourceLocations("classpath:/META-INF/resources/webjars/")

    //   // Add default resource handlers for templates
    //   registry.addResourceHandler("/templates/**")
    //     .addResourceLocations("classpath:/templates/")
    // }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Add default resource handlers for static resources
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")

        // Add default resource handlers for templates
        registry.addResourceHandler("/templates/**")
            .addResourceLocations("classpath:/templates/")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // 添加对404错误页面的映射
        registry.addViewController("/error-404").setViewName("forward:/templates/error-404.html")
    }
}
