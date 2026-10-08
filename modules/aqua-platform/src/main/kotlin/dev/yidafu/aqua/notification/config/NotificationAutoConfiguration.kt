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

package dev.yidafu.aqua.notification.config

import dev.yidafu.aqua.notification.channel.ws.NotificationWsHandler
import jakarta.jms.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * 通知模块自动配置(PRD 13-notification.md)
 * 扫描 dev.yidafu.aqua.notification 包;注册 WebSocket 推送端点 /ws/notifications;
 * 通知分发使用独立线程池,WebSocket 推送与微信订阅消息发送异步执行,不阻塞 MQ 消费线程。
 *
 * notificationTopicJmsListenerContainerFactory:广播主题(broadcast-events)专用监听容器,
 * 每个服务一个独立非持久订阅(concurrency=1),确保每个服务都收到推送事件并推送到本服务会话。
 */
@Configuration
@EnableScheduling
@EnableWebSocket
@ComponentScan(basePackages = ["dev.yidafu.aqua.notification"])
class NotificationAutoConfiguration(
  private val notificationWsHandler: NotificationWsHandler,
) : WebSocketConfigurer {
  @Bean("notificationDispatchExecutor")
  fun notificationDispatchExecutor(): TaskExecutor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = 2
    executor.maxPoolSize = 8
    executor.queueCapacity = 500
    executor.setThreadNamePrefix("notification-dispatch-")
    executor.initialize()
    return executor
  }

  @Bean("notificationTopicJmsListenerContainerFactory")
  fun notificationTopicJmsListenerContainerFactory(connectionFactory: ConnectionFactory): DefaultJmsListenerContainerFactory {
    val factory = DefaultJmsListenerContainerFactory()
    factory.setConnectionFactory(connectionFactory)
    factory.setConcurrency("1")
    factory.setSessionTransacted(true)
    return factory
  }

  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry
      .addHandler(notificationWsHandler, WS_ENDPOINT)
      .setAllowedOrigins("*")
  }

  companion object {
    const val WS_ENDPOINT = "/ws/notifications"
  }
}
