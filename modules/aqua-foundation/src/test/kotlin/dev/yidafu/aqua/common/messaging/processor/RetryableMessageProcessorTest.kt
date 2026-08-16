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

package dev.yidafu.aqua.common.messaging.processor

import dev.yidafu.aqua.common.messaging.config.SimplifiedMessagingProperties
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RetryableMessageProcessorTest :
  FunSpec({
    fun event() = DomainEvent(eventType = "ORDER_CREATED", payload = "{}")

    fun properties(
      maxAttempts: Int = 3,
      initialInterval: Long = 5,
      multiplier: Double = 2.0,
      maxInterval: Long = 10,
    ): SimplifiedMessagingProperties {
      val props = SimplifiedMessagingProperties()
      props.artemis.retry.maxAttempts = maxAttempts
      props.artemis.retry.initialInterval = initialInterval
      props.artemis.retry.multiplier = multiplier
      props.artemis.retry.maxInterval = maxInterval
      return props
    }

    test("processWithRetry should succeed on first attempt") {
      val processor = RetryableMessageProcessor(properties())
      var calls = 0
      val result =
        processor.processWithRetry(event()) {
          calls++
        }
      result shouldBe true
      calls shouldBe 1
    }
    test("processWithRetry should succeed after retries") {
      val processor = RetryableMessageProcessor(properties(maxAttempts = 3))
      var calls = 0
      val result =
        processor.processWithRetry(event()) {
          calls++
          if (calls < 3) {
            throw IllegalStateException("transient failure")
          }
        }
      result shouldBe true
      calls shouldBe 3
    }
    test("processWithRetry should return false after exhausting attempts") {
      val processor = RetryableMessageProcessor(properties(maxAttempts = 2, initialInterval = 1))
      var calls = 0
      val result =
        processor.processWithRetry(event()) {
          calls++
          throw IllegalStateException("always fails")
        }
      result shouldBe false
      calls shouldBe 2
    }
    test("processWithRetry should cap delay at max interval") {
      val processor = RetryableMessageProcessor(properties(maxAttempts = 4, initialInterval = 3, multiplier = 10.0, maxInterval = 7))
      var calls = 0
      val result =
        processor.processWithRetry(event()) {
          calls++
          throw IllegalStateException("always fails")
        }
      result shouldBe false
      calls shouldBe 4
    }
    test("processWithRetry should return false when interrupted during wait") {
      val processor = RetryableMessageProcessor(properties(maxAttempts = 3, initialInterval = 1000))
      var calls = 0
      val result =
        processor.processWithRetry(event()) {
          calls++
          if (calls == 1) {
            Thread.currentThread().interrupt()
          }
          throw IllegalStateException("fail")
        }
      result shouldBe false
      Thread.interrupted() shouldBe true // clear the interrupt flag
    }
    test("processBatchWithRetry should return per event results") {
      val processor = RetryableMessageProcessor(properties(maxAttempts = 2, initialInterval = 1))
      val events = listOf(event(), event())
      var calls = 0
      val results =
        processor.processBatchWithRetry(events) {
          calls++
          if (calls == 1) {
            throw IllegalStateException("first fails once")
          }
        }
      results shouldBe listOf(true, true)
    }
    test("processBatchWithRetry with empty list should return empty") {
      val processor = RetryableMessageProcessor(properties())
      processor.processBatchWithRetry(emptyList()) { } shouldBe (emptyList<Boolean>())
    }
  })
