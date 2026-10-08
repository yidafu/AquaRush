/**
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

package dev.yidafu.aqua.analytics.reconciliation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 对账异步执行线程池(reconciliationExecutor):
 * 支付/退款/结算对账任务经 @Async("reconciliationExecutor") 独立线程池执行,
 * 不阻塞调度线程与请求线程。
 */
@Configuration
class ReconciliationAsyncConfig {
  @Bean("reconciliationExecutor")
  fun reconciliationExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 2
    executor.maxPoolSize = 4
    executor.queueCapacity = 50
    executor.setThreadNamePrefix("reconciliation-")
    executor.initialize()
    return executor
  }
}
