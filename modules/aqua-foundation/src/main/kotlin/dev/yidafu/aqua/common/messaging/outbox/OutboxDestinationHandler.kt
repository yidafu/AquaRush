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

/**
 * outbox 目的处理器:每个 destination 一个处理契约(替代旧 JMS 监听注解)。
 * 实现类注册为 Spring Bean,[OutboxPoller] 按 [destinations] 建路由表。
 *
 * 契约:处理失败**抛出异常** → 轮询器按指数退避重试,达上限置 DEAD;
 * 处理逻辑必须幂等(at-least-once,与 Artemis 重投语义一致)。
 */
interface OutboxDestinationHandler {
  /** 本处理器负责的 destination(可多个,如四个业务队列共用 EventProcessor 分发)。 */
  val destinations: Set<String>

  /** 处理一条认领的事件;失败抛异常。 */
  fun handle(record: OutboxEventRecord)
}
