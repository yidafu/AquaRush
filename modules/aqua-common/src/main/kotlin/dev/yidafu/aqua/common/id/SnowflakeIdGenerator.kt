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

package dev.yidafu.aqua.common.id

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Snowflake ID generator implementation
 *
 * Structure: 64-bit long
 * - 1 bit: unused (sign bit)
 * - 41 bits: timestamp (milliseconds since epoch)
 * - 10 bits: machine ID (0-1023)
 * - 12 bits: sequence (0-4095)
 */
class SnowflakeIdGenerator(
  private val machineId: Long = 1L, // Default machine ID, should be configured per instance
) {
  companion object {
    private const val EPOCH = 1609459200000L // 2021-01-01 00:00:00 UTC
    private const val MACHINE_ID_BITS = 10
    private const val SEQUENCE_BITS = 12
    private const val MAX_MACHINE_ID = -1L xor (-1L shl MACHINE_ID_BITS)
    private const val MAX_SEQUENCE = -1L xor (-1L shl SEQUENCE_BITS)
    private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS
    private const val MACHINE_ID_SHIFT = SEQUENCE_BITS

    // Default singleton instance
    @Volatile
    private var defaultInstance: SnowflakeIdGenerator? = null

    fun getInstance(): SnowflakeIdGenerator =
      defaultInstance ?: synchronized(this) {
        defaultInstance ?: SnowflakeIdGenerator().also { defaultInstance = it }
      }

    fun initialize(machineId: Long) {
      require(machineId >= 0 && machineId <= MAX_MACHINE_ID) {
        "Machine ID must be between 0 and $MAX_MACHINE_ID"
      }
      defaultInstance = SnowflakeIdGenerator(machineId)
    }
  }

  init {
    require(machineId >= 0 && machineId <= MAX_MACHINE_ID) {
      "Machine ID must be between 0 and $MAX_MACHINE_ID"
    }
  }

  private var lastTimestamp = -1L
  private val sequence = AtomicLong(0)

  /**
   * Generate a new Snowflake ID
   */
  fun nextId(): Long {
    val currentTimestamp = timestamp()

    if (currentTimestamp < lastTimestamp) {
      throw IllegalStateException("Clock moved backwards. Refusing to generate ID.")
    }

    val currentSequence =
      if (currentTimestamp == lastTimestamp) {
        sequence.incrementAndGet() % MAX_SEQUENCE
      } else {
        sequence.set(0)
        0
      }

    lastTimestamp = currentTimestamp

    return (currentTimestamp - EPOCH) shl TIMESTAMP_SHIFT or
      (machineId shl MACHINE_ID_SHIFT) or
      currentSequence
  }

  /**
   * Generate a new Snowflake ID using the default instance
   */
  fun generate(): Long = getInstance().nextId()

  private fun timestamp(): Long = System.currentTimeMillis()

  /**
   * Extract timestamp from a Snowflake ID
   */
  fun extractTimestamp(id: Long): Instant = Instant.ofEpochMilli((id shr TIMESTAMP_SHIFT) + EPOCH)

  /**
   * Extract machine ID from a Snowflake ID
   */
  fun extractMachineId(id: Long): Long = (id shr MACHINE_ID_SHIFT) and MAX_MACHINE_ID

  /**
   * Extract sequence from a Snowflake ID
   */
  fun extractSequence(id: Long): Long = id and MAX_SEQUENCE
}
