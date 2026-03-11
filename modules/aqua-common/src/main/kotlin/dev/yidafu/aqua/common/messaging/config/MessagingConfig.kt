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

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

/**
 * Spring Messaging配置
 * 配置ActiveMQ Artemis消息队列的相关Bean和线程池
 */
@Configuration
@EnableConfigurationProperties(SimplifiedMessagingProperties::class)
@EnableAsync
class MessagingConfig {
  @Bean
  @Primary
  fun simplifiedMessagingProperties(): SimplifiedMessagingProperties = SimplifiedMessagingProperties()

  /**
   * 事件处理的异步执行器
   */
  @Bean("taskExecutor")
  fun taskExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 4
    executor.maxPoolSize = 16
    executor.queueCapacity = 1000
    executor.setThreadNamePrefix("event-processor-")
    executor.setWaitForTasksToCompleteOnShutdown(true)
    executor.setAwaitTerminationSeconds(30)
    executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
    executor.initialize()
    return executor
  }
}
