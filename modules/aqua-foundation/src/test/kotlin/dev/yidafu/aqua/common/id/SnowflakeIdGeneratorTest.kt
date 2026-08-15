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

package dev.yidafu.aqua.common.id

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SnowflakeIdGeneratorTest {
  @Test
  fun `nextId should generate positive unique ids`() {
    val generator = SnowflakeIdGenerator()
    val ids = (1..1000).map { generator.nextId() }
    assertEquals(1000, ids.toSet().size)
    assertTrue(ids.all { it > 0 })
  }

  @Test
  fun `ids should be monotonically increasing`() {
    val generator = SnowflakeIdGenerator()
    var previous = generator.nextId()
    repeat(100) {
      val current = generator.nextId()
      assertTrue(current > previous)
      previous = current
    }
  }

  @Test
  fun `generate should delegate to default instance`() {
    val generator = SnowflakeIdGenerator()
    assertNotNull(generator.generate())
    assertTrue(generator.generate() > 0)
  }

  @Test
  fun `extractTimestamp should recover time close to now`() {
    val generator = SnowflakeIdGenerator()
    val before = Instant.now().minus(1, ChronoUnit.SECONDS)
    val id = generator.nextId()
    val after = Instant.now().plus(1, ChronoUnit.SECONDS)
    val ts = generator.extractTimestamp(id)
    assertTrue(!ts.isBefore(before))
    assertTrue(!ts.isAfter(after))
  }

  @Test
  fun `extractMachineId should recover configured machine id`() {
    val generator = SnowflakeIdGenerator(machineId = 42L)
    val id = generator.nextId()
    assertEquals(42L, generator.extractMachineId(id))
  }

  @Test
  fun `extractSequence should be within bounds`() {
    val generator = SnowflakeIdGenerator()
    val id = generator.nextId()
    val seq = generator.extractSequence(id)
    assertTrue(seq in 0..4095)
  }

  @Test
  fun `constructor should reject negative machine id`() {
    assertThrows<IllegalArgumentException> { SnowflakeIdGenerator(machineId = -1L) }
  }

  @Test
  fun `constructor should reject machine id above max`() {
    assertThrows<IllegalArgumentException> { SnowflakeIdGenerator(machineId = 1024L) }
  }

  @Test
  fun `initialize should validate machine id range`() {
    assertThrows<IllegalArgumentException> { SnowflakeIdGenerator.initialize(-5L) }
    assertThrows<IllegalArgumentException> { SnowflakeIdGenerator.initialize(5000L) }
  }

  @Test
  fun `getInstance should return same singleton`() {
    val a = SnowflakeIdGenerator.getInstance()
    val b = SnowflakeIdGenerator.getInstance()
    assertEquals(a, b)
  }

  @Test
  fun `initialize should replace default instance`() {
    SnowflakeIdGenerator.initialize(7L)
    val instance = SnowflakeIdGenerator.getInstance()
    val id = instance.nextId()
    assertEquals(7L, instance.extractMachineId(id))
  }

  @Test
  fun `default id generator should produce positive ids`() {
    val generator = DefaultIdGenerator()
    assertTrue(generator.generate() > 0)
  }
}
