/*
 * AquaRush Client Service
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

package dev.yidafu.aqua.client

import dev.yidafu.aqua.common.cache.CacheAutoConfiguration
import dev.yidafu.aqua.storage.config.StorageProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.TimeZone

@SpringBootApplication(
  scanBasePackages = [
    "dev.yidafu.aqua",
  ],
  exclude = [CacheAutoConfiguration::class],
)
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties(StorageProperties::class)
class AquaClientApplication

fun main(args: Array<String>) {
  // 统计聚合时区统一 Asia/Shanghai(P2-8,PRD 08-statistics.md §域级口径):
  // 实体默认时间戳(LocalDateTime.now())与统计日界均依赖 JVM 默认时区,显式钉死不随部署环境漂移。
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
  runApplication<AquaClientApplication>(*args)
}
