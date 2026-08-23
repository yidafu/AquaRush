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

package dev.yidafu.aqua.common.messaging.publisher

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.jms.Message
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessagePostProcessor

/**
 * Artemis 事件发布器的有界发送测试:
 * broker 流控(磁盘满/地址满)会让生产者无限阻塞,这里验证发送被限制在超时窗口内,
 * 超时返回 false 而非无限挂起 —— 保证订单接单/派单等业务请求不被消息队列拖垮。
 */
class ArtemisEventPublisherTest :
  FunSpec({
    context("sendBounded") {
      test("publishSync 在 broker 无限阻塞时于超时后返回 false,而非无限挂起") {
        val template = mockk<JmsTemplate>()
        every { template.convertAndSend(any<String>(), any(), any()) } answers {
          Thread.sleep(60_000) // 模拟 Artemis 流控 credits 无限阻塞
        }
        val publisher = ArtemisEventPublisher(template, sendTimeoutMillis = 300)
        val start = System.currentTimeMillis()
        val result = publisher.publishSync(domainEvent())
        val elapsed = System.currentTimeMillis() - start

        result shouldBe false
        elapsed shouldBeLessThan 10_000
      }

      test("publishSync 正常发送返回 true") {
        val template = mockk<JmsTemplate>()
        every { template.convertAndSend(any<String>(), any(), any()) } answers { Unit }
        val publisher = ArtemisEventPublisher(template, sendTimeoutMillis = 300)
        publisher.publishSync(domainEvent()) shouldBe true
      }

      test("publishSync 发送抛异常返回 false") {
        val template = mockk<JmsTemplate>()
        every { template.convertAndSend(any<String>(), any(), any()) } throws RuntimeException("broker down")
        val publisher = ArtemisEventPublisher(template, sendTimeoutMillis = 300)
        publisher.publishSync(domainEvent()) shouldBe false
      }

      test("publishDelayed 应用延迟投递头") {
        val template = mockk<JmsTemplate>()
        every { template.convertAndSend(any<String>(), any(), any<MessagePostProcessor>()) } answers {
          val message = mockk<Message>(relaxed = true)
          thirdArg<MessagePostProcessor>().postProcessMessage(message)
        }
        val publisher = ArtemisEventPublisher(template, sendTimeoutMillis = 300)
        publisher.publishDelayed(domainEvent(), deliveryTimeMillis = 1_234_567L) shouldBe true

        verify { template.convertAndSend(any<String>(), any(), any<MessagePostProcessor>()) }
      }
    }
  })

private fun domainEvent(): DomainEvent =
  DomainEvent(
    aggregateId = "100",
    eventType = DomainEventType.DELIVERY_ASSIGNED.value,
    payload = "{}",
  )
