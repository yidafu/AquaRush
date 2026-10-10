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

package dev.yidafu.aqua.common.messaging.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Transactional Outbox 配置(docs/compose/spec/outbox-messaging.md)。
 * 两服务共享 outbox 表,通过 [destinations] 订阅差异与 [instance] 身份隔离消费。
 */
@ConfigurationProperties(prefix = "aqua.outbox")
data class OutboxProperties(
  /** 是否启用 outbox 轮询分发(发布侧 INSERT 由 aqua.messaging.enabled 控制)。 */
  var enabled: Boolean = false,
  /**
   * 本服务订阅的 destination 列表。
   * admin = 全部队列(含 payment-expiry-events)+broadcast-events;
   * client = 除 payment-expiry-events 外 +broadcast-events。
   */
  var destinations: List<String> = emptyList(),
  /** 广播扇出名单:每个成员各写一行(值约定与服务 aqua.mode 一致:admin/client)。 */
  var subscribers: List<String> = listOf("admin", "client"),
  /** 本服务实例身份(= aqua.mode),用于广播行归属与锁归属。 */
  var instance: String = "",
  /** 轮询间隔(毫秒)。 */
  var pollIntervalMs: Long = 300,
  /** 单次认领批量。 */
  var batchSize: Int = 10,
  /** 最大处理次数(含首次),达到置 DEAD。 */
  var maxAttempts: Int = 8,
  /** 退避初始间隔(毫秒)。 */
  var initialBackoffMs: Long = 1000,
  /** 退避倍数。 */
  var backoffMultiplier: Double = 2.0,
  /** 退避上限(毫秒)。 */
  var maxBackoffMs: Long = 60000,
  /** PROCESSING 可见性超时(毫秒):进程被杀后行可被恢复认领的时间。 */
  var visibilityTimeoutMs: Long = 300000,
  /** DONE 行保留天数(超期由清理任务删除)。 */
  var doneRetentionDays: Long = 7,
)
