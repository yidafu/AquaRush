package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.domain.model.AdminPermission
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.security.toSimpleGrantedAuthorities
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
    val authorities = determineAuthorities(userType)

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

  private fun determineAuthorities(userType: String): List<SimpleGrantedAuthority> {
    val authorities = mutableListOf<SimpleGrantedAuthority>()

    // Add role based on user type
    authorities.add(SimpleGrantedAuthority("ROLE_$userType"))

    // Add specific permissions based on user type using AdminPermission enum
    val permissions = when (userType) {
      "ADMIN" -> setOf(
        AdminPermission.USER_READ,
        AdminPermission.USER_WRITE,
        AdminPermission.ORDER_READ,
        AdminPermission.ORDER_WRITE,
        AdminPermission.PRODUCT_READ,
        AdminPermission.PRODUCT_WRITE,
        AdminPermission.DELIVERY_READ,
        AdminPermission.DELIVERY_WRITE,
        AdminPermission.PAYMENT_READ,
        AdminPermission.PAYMENT_WRITE,
        AdminPermission.STATISTICS_READ,
      )

      "WORKER" -> setOf(
        AdminPermission.DELIVERY_READ,
        AdminPermission.DELIVERY_WRITE,
        AdminPermission.ORDER_READ,
      )

      "USER" -> setOf(
        AdminPermission.USER_READ,
        AdminPermission.USER_WRITE,
        AdminPermission.ORDER_READ,
        AdminPermission.ORDER_WRITE,
        AdminPermission.PRODUCT_READ,
      )

      else -> emptySet()
    }

    authorities.addAll(permissions.toSimpleGrantedAuthorities())

    return authorities
  }
}
