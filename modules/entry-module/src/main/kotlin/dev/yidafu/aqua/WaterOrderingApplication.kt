package dev.yidafu.aqua

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "dev.yidafu.aqua.user",
        "dev.yidafu.aqua.product",
        "dev.yidafu.aqua.order",
        "dev.yidafu.aqua.delivery",
        "dev.yidafu.aqua.payment",
        "dev.yidafu.aqua.statistics",
        "dev.yidafu.aqua.config"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "dev.yidafu.aqua.user.domain.repository",
        "dev.yidafu.aqua.product.domain.repository",
        "dev.yidafu.aqua.order.domain.repository",
        "dev.yidafu.aqua.delivery.domain.repository"
    ]
)
@EnableScheduling
class WaterOrderingApplication

fun main(args: Array<String>) {
    runApplication<WaterOrderingApplication>(*args)
}
