package dev.yidafu.aqua.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 全局 PasswordEncoder Bean。
 *
 * 独立于各服务 SecurityConfig 之外，避免 SecurityConfig → AdminUserDetailsService → AdminServiceImpl → PasswordEncoder(Bean 在 SecurityConfig)
 * 形成的循环依赖。
 */
@Configuration
class PasswordEncoderConfig {
  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
