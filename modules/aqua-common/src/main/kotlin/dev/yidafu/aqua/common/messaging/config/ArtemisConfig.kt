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

package dev.yidafu.aqua.common.messaging.config

import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms

/**
 * ActiveMQ Artemis配置类
 * 依赖Spring Boot的自动配置来管理嵌入式ActiveMQ Artemis Broker
 * 配置通过application.yml中的artemis部分进行管理
 */
@Configuration
@EnableJms
class ArtemisConfig {
  // 移除了自定义的EmbeddedActiveMQ Bean，改为使用Spring Boot的自动配置
  // 这样可以避免JMS监听器在Broker启动前尝试连接的问题
}
