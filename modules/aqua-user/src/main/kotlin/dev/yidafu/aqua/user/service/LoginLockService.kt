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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * 登录失败锁定(PRD 11-system-settings.md §F-SYS-004,AC-SYS-004-02):
 * 连续错误密码达 [SystemSettingKeys.LOGIN_FAIL_LOCK_THRESHOLD](默认 5,1-99)后
 * 账号锁定 [SystemSettingKeys.LOGIN_LOCK_DURATION_MINUTES](默认 30,≥1 分钟),锁定期内不可登录;
 * 登录成功清零;锁定到期自动重置计数(重新获得完整失败次数)。
 *
 * 计数驻内存(单门店单服务器定位,PRD 01-overview;重启即重置,纵深防御而非唯一屏障);
 * 上限 1024 个账号,超出时清理 1 小时前的过期失败痕迹,防止登录端点刷账号撑爆内存。
 */
@Service
class LoginLockService(
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  private data class FailState(
    val count: Int,
    val lockedUntil: Instant?,
    val lastFailedAt: Instant,
  )

  private val states = ConcurrentHashMap<String, FailState>()

  /**
   * 剩余锁定秒数;0 表示未锁定(含锁定已到期——到期顺带重置失败计数)。
   */
  fun lockedRemainingSeconds(username: String): Long {
    val state =
      states.compute(username) { _, existing ->
        val until = existing?.lockedUntil ?: return@compute existing
        if (until.isAfter(Instant.now())) existing else existing.copy(lockedUntil = null, count = 0)
      } ?: return 0
    val until = state.lockedUntil ?: return 0
    return Instant.now().until(until, ChronoUnit.SECONDS).coerceAtLeast(0)
  }

  /**
   * 登录失败计数;达阈值即锁定。锁定期内的失败不重复计数(不延长锁定)。
   */
  fun recordFailure(username: String) {
    val threshold =
      systemConfigQueryApiService
        .getLongValue(SystemSettingKeys.LOGIN_FAIL_LOCK_THRESHOLD, 5L)
        .coerceIn(1, 99)
    val durationMinutes =
      systemConfigQueryApiService
        .getLongValue(SystemSettingKeys.LOGIN_LOCK_DURATION_MINUTES, 30L)
        .coerceAtLeast(1)

    evictStale()

    states.compute(username) { _, existing ->
      val now = Instant.now()
      val lockedUntil = existing?.lockedUntil
      if (lockedUntil != null && lockedUntil.isAfter(now)) {
        existing
      } else {
        val count = (existing?.count ?: 0) + 1
        if (count >= threshold) {
          FailState(count, now.plus(durationMinutes, ChronoUnit.MINUTES), now)
        } else {
          FailState(count, null, now)
        }
      }
    }
  }

  /** 登录成功清零失败计数并解除锁定。 */
  fun clear(username: String) {
    states.remove(username)
  }

  /** 超出追踪上限时清理 1 小时前且已解锁的失败痕迹(防登录端点刷账号撑爆内存)。 */
  private fun evictStale() {
    if (states.size <= MAX_TRACKED) return
    val cutoff = Instant.now().minus(1, ChronoUnit.HOURS)
    states.entries.removeIf { (_, state) ->
      state.lastFailedAt.isBefore(cutoff) && (state.lockedUntil == null || !state.lockedUntil.isAfter(Instant.now()))
    }
  }

  companion object {
    private const val MAX_TRACKED = 1024
  }
}
