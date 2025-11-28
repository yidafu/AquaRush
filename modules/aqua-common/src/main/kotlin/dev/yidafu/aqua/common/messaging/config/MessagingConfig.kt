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
  fun simplifiedMessagingProperties(): SimplifiedMessagingProperties {
    return SimplifiedMessagingProperties()
  }

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

  /**
   * 高频事件处理执行器（专门用于内存队列）
   */
  @Bean("highFrequencyExecutor")
  fun highFrequencyExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 2
    executor.maxPoolSize = 8
    executor.queueCapacity = 500
    executor.setThreadNamePrefix("high-freq-event-")
    executor.setWaitForTasksToCompleteOnShutdown(true)
    executor.setAwaitTerminationSeconds(10)
    executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
    executor.initialize()
    return executor
  }

  /**
   * 低频事件处理执行器（专门用于Outbox队列）
   */
  @Bean("lowFrequencyExecutor")
  fun lowFrequencyExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 1
    executor.maxPoolSize = 4
    executor.queueCapacity = 200
    executor.setThreadNamePrefix("low-freq-event-")
    executor.setWaitForTasksToCompleteOnShutdown(true)
    executor.setAwaitTerminationSeconds(60)
    executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
    executor.initialize()
    return executor
  }
}
