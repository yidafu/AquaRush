package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class ClientUserDetailsService(
  private val userRepository: UserRepository,
) : UserDetailsService {
  val logger = LoggerFactory.getLogger(ClientUserDetailsService::class.java)

  override fun loadUserByUsername(username: String): UserDetails {
    logger.info("loadUserByUsername $username")
    // For JWT authentication, username is the OpenID
    val user =
      userRepository.findByWechatOpenId(username)
        ?: throw UsernameNotFoundException("User not found with openid: $username")

    // Determine user type and authorities based on user data
    val userType = determineUserType(user)
    val authorities = determineAuthorities(user, userType)

    return UserPrincipal(
      id = user.id!!,
      _username = user.wechatOpenId,
      userType = userType,
      _authorities = authorities,
    )
  }

  private fun determineUserType(user: UserModel): String {
    // Check if user is an admin (this could be based on a field in User entity)
    // For now, we'll use a simple logic - you can extend this based on your requirements
    return if (user.phone?.startsWith("admin") == true) {
      "ADMIN"
    } else if (user.phone?.startsWith("worker") == true) {
      "WORKER"
    } else {
      "USER"
    }
  }

  private fun determineAuthorities(
    user: UserModel,
    userType: String,
  ): List<SimpleGrantedAuthority> {
    val authorities = mutableListOf<SimpleGrantedAuthority>()

    // Add role based on user type
    authorities.add(SimpleGrantedAuthority("ROLE_$userType"))

    // Add specific permissions based on user type
    when (userType) {
      "ADMIN" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PAYMENT_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PAYMENT_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_STATISTICS_READ"))
      }

      "WORKER" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
      }

      "USER" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_READ"))
      }
    }

    return authorities
  }
}
