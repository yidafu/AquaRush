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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit

class SnowflakeIdGeneratorTest :
  FunSpec({

    test("nextId should generate positive unique ids") {
      val generator = SnowflakeIdGenerator()
      val ids = (1..1000).map { generator.nextId() }
      ids.toSet().size shouldBe 1000
      ids.all { it > 0 } shouldBe true
    }
    test("ids should be monotonically increasing") {
      val generator = SnowflakeIdGenerator()
      var previous = generator.nextId()
      repeat(100) {
        val current = generator.nextId()
        (current > previous) shouldBe true
        previous = current
      }
    }
    test("generate should delegate to default instance") {
      val generator = SnowflakeIdGenerator()
      generator.generate().shouldNotBeNull()
      (generator.generate() > 0) shouldBe true
    }
    test("extractTimestamp should recover time close to now") {
      val generator = SnowflakeIdGenerator()
      val before = Instant.now().minus(1, ChronoUnit.SECONDS)
      val id = generator.nextId()
      val after = Instant.now().plus(1, ChronoUnit.SECONDS)
      val ts = generator.extractTimestamp(id)
      !ts.isBefore(before) shouldBe true
      !ts.isAfter(after) shouldBe true
    }
    test("extractMachineId should recover configured machine id") {
      val generator = SnowflakeIdGenerator(machineId = 42L)
      val id = generator.nextId()
      generator.extractMachineId(id) shouldBe 42L
    }
    test("extractSequence should be within bounds") {
      val generator = SnowflakeIdGenerator()
      val id = generator.nextId()
      val seq = generator.extractSequence(id)
      (seq in 0..4095) shouldBe true
    }
    test("constructor should reject negative machine id") {
      shouldThrow<IllegalArgumentException> { SnowflakeIdGenerator(machineId = -1L) }
    }
    test("constructor should reject machine id above max") {
      shouldThrow<IllegalArgumentException> { SnowflakeIdGenerator(machineId = 1024L) }
    }
    test("initialize should validate machine id range") {
      shouldThrow<IllegalArgumentException> { SnowflakeIdGenerator.initialize(-5L) }
      shouldThrow<IllegalArgumentException> { SnowflakeIdGenerator.initialize(5000L) }
    }
    test("getInstance should return same singleton") {
      val a = SnowflakeIdGenerator.getInstance()
      val b = SnowflakeIdGenerator.getInstance()
      b shouldBe a
    }
    test("initialize should replace default instance") {
      SnowflakeIdGenerator.initialize(7L)
      val instance = SnowflakeIdGenerator.getInstance()
      val id = instance.nextId()
      instance.extractMachineId(id) shouldBe 7L
    }
    test("default id generator should produce positive ids") {
      val generator = DefaultIdGenerator()
      (generator.generate() > 0) shouldBe true
    }
  })
