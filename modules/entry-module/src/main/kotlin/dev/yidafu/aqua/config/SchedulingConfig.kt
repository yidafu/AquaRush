package dev.yidafu.aqua.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class SchedulingConfig {
    // 启用定时任务支持，用于 Outbox 事件轮询
}
