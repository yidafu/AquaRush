package dev.yidafu.aqua.delivery.service.impl

import cn.binarywang.wx.miniapp.api.WxMaService
import dev.yidafu.aqua.api.dto.DeliveryLoginResponse
import dev.yidafu.aqua.api.dto.DeliveryWorkerInfo
import dev.yidafu.aqua.api.query.DeliveryLoginRequest
import dev.yidafu.aqua.api.service.delivery.DeliveryAuthService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.JwtTokenException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.security.toPermissionAuthorities
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.ext.toSimpleGrantedAuthority
import dev.yidafu.aqua.user.service.dto.toDeliveryWorkerInfo
import me.chanjar.weixin.common.error.WxErrorException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DeliveryAuthServiceImpl(
  private val wxMaService: WxMaService,
  private val jwtTokenService: JwtTokenService,
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val adminRepository: AdminRepository,
  private val userRepository: UserRepository,
  private val bizLogger: BizLogger,
) : DeliveryAuthService {
  private val logger = LoggerFactory.getLogger(DeliveryAuthServiceImpl::class.java)

  private fun getWorkInfo(deliveryWorkId: Long): DeliveryWorkerInfo {
    val worker = deliveryWorkerRepository.findById(deliveryWorkId).orElseThrow()
    val admin = adminRepository.findById(worker.adminId).orElseThrow()
    return DeliveryWorkerInfo(
      id = worker.id!!,
      name = worker.name,
      phone = worker.phone,
      avatarUrl = worker.avatarUrl,
      role = admin.role.toString(),
    )
  }

  override fun login(request: DeliveryLoginRequest): DeliveryLoginResponse {
    try {
      // Exchange code for OpenID
      val openId = exchangeCodeForOpenId(request.code)
      logger.info("WeChat login: openId={}", openId)

      // Check if worker already exists
      val existingWorker = deliveryWorkerRepository.findByWechatOpenId(openId)
      if (existingWorker != null) {
        // Worker already bound, generate token
        val token = generateToken(existingWorker.adminId)

        // 记录配送员登录日志
        bizLogger.logLogin(
          userId = existingWorker.id!!.toString(),
          username = existingWorker.name,
          loginMethod = "WECHAT_DELIVERY",
          success = true,
        )

        return DeliveryLoginResponse(
          token = token,
          refreshToken = null,
          needBindPhone = false,
          workerInfo = getWorkInfo(existingWorker.id!!),
          message = "登录成功",
        )
      }

      // Worker not bound, need to bind phone
      // 先在 users 表中创建用户记录，以便后续 bindPhone 时能找到用户
      val existingUser = userRepository.findByWechatOpenId(openId)
      if (existingUser == null) {
        val newUser =
          UserModel(
            wechatOpenId = openId,
            status = UserStatus.ACTIVE,
            role = UserRole.NONE,
          )
        userRepository.save(newUser)
        logger.info("Created user record for delivery worker: openId={}", openId)
      }

      // Generate pending token with limited permissions
      val pendingToken = generatePendingToken(openId)
      return DeliveryLoginResponse(
        token = pendingToken,
        refreshToken = null,
        needBindPhone = true,
        workerInfo = null,
        message = "请绑定手机号",
      )
    } catch (e: Exception) {
      logger.error("Delivery worker login failed", e)
      // 记录登录失败日志
      bizLogger.logLogin(
        userId = "0",
        username = "asynounmes",
        loginMethod = "WECHAT_DELIVERY",
        success = false,
        additionalData = mapOf("error" to (e.message ?: "unknown error")),
      )
      throw BadRequestException("登录失败: ${e.message}")
    }
  }

  /**
   * Bind phone number using JWT token
   * OpenID is extracted from the JWT token (pending token)
   */
  @Transactional
  override fun bindPhone(
    token: String,
    phoneNumber: String,
  ): DeliveryLoginResponse {
    // Extract token from Bearer header
    val actualToken =
      if (token.startsWith("Bearer ")) {
        token.substring(7)
      } else {
        token
      }

    // Validate and parse token to get openId
    val userPrincipal =
      try {
        jwtTokenService.getUserPrincipalFromToken(actualToken)
      } catch (e: JwtTokenException) {
        logger.warn("Invalid token: {}", e.message)
        throw BadRequestException("登录态无效，请重新登录")
      }

    if (userPrincipal == null) {
      throw BadRequestException("登录态无效，请重新登录")
    }

    // Check if token is expired
    if (jwtTokenService.isTokenExpired(actualToken)) {
      throw BadRequestException("登录态已过期，请重新登录")
    }

    // Extract openId from token (for pending workers, username is the openId)
    val openId = userPrincipal.username
    if (openId.isBlank()) {
      throw BadRequestException("无效的登录信息，请重新登录")
    }

    logger.info("Binding phone for openId: {}", openId)

    // Verify phone number belongs to admin or delivery worker
    val admin = adminRepository.findByPhone(phoneNumber)
    val isAdmin = admin != null

    if (!isAdmin) {
      throw BadRequestException("该手机号未注册为管理员或送水员，请联系管理员")
    }

    // 根据手机查询送水员
    var worker =
      deliveryWorkerRepository.findByPhone(phoneNumber)
        ?: throw BadRequestException("该手机号未注册为管理员或送水员，请联系管理员")

    // 检查是否被绑定
    if (worker.wechatOpenId.isNotBlank()) {
      throw BadRequestException("该手机号已被绑定，请联系管理员")
    }

    // 查找或创建用户记录
    val user =
      userRepository.findByWechatOpenId(openId)
        ?: throw BadRequestException("微信用户不存在")

    // 绑定用户ID和手机号
    worker.userId = user.id
    worker.phone = phoneNumber
    worker.wechatOpenId = openId
    worker.adminId = admin.id!!
    worker = deliveryWorkerRepository.save(worker)

    admin.deliveryWorkerId = worker.id
    admin.userId = user.id
    admin.lastLoginAt = LocalDateTime.now()
    adminRepository.save(admin)

    logger.info(
      "Updated existing worker: id={}, phone={}, openId={}, userId={}",
      worker.id,
      phoneNumber,
      openId,
      user.id,
    )

    // Generate token
    val newToken = generateToken(admin.id!!)

    // 记录配送员手机绑定日志
    bizLogger.logLogin(
      userId = worker.id!!.toString(),
      username = worker.name ?: phoneNumber,
      loginMethod = "PHONE_BIND",
      success = true,
      additionalData = mapOf("phoneNumber" to phoneNumber),
    )

    return DeliveryLoginResponse(
      token = newToken,
      refreshToken = null,
      needBindPhone = false,
      workerInfo = worker.toDeliveryWorkerInfo(),
      message = "绑定成功",
    )
  }

  /**
   * Exchange WeChat code for OpenID
   */
  private fun exchangeCodeForOpenId(code: String): String =
    try {
      val session = wxMaService.userService.getSessionInfo(code)
      session.openid ?: throw IllegalStateException("OpenID is null")
    } catch (e: WxErrorException) {
      logger.error("WeChat code exchange failed", e)
      throw BadRequestException("微信登录失败: ${e.message}")
    }

  /**
   * Generate JWT token for delivery worker
   */
  private fun generateToken(adminId: Long): String {
    val admin = adminRepository.findById(adminId).orElseThrow { UserNotFoundException("管理员不存在") }
    val userPrincipal =
      UserPrincipal(
        id = admin.id!!,
        _username = admin.username,
        userType = admin.role.name,
        _authorities = admin.role.toPermissionAuthorities(),
      )
    return jwtTokenService.generateAccessToken(userPrincipal)
  }

  /**
   * Generate JWT token for pending delivery worker (not yet bound to phone)
   */
  private fun generatePendingToken(openId: String): String {
    val authorities = listOf(AdminRoleModel.DELIVERY_WORKER.toSimpleGrantedAuthority())
    val userPrincipal =
      UserPrincipal(
        id = 0L, // 0 indicates pending/unbound worker
        _username = openId,
        userType = AdminRoleModel.DELIVERY_WORKER.name,
        _authorities = authorities,
      )
    return jwtTokenService.generateAccessToken(userPrincipal)
  }

  override fun checkAuthStatus(token: String): DeliveryLoginResponse {
    // Extract token from Bearer header
    val actualToken =
      if (token.startsWith("Bearer ")) {
        token.substring(7)
      } else {
        token
      }

    // Validate and parse token
    val userPrincipal =
      try {
        jwtTokenService.getUserPrincipalFromToken(actualToken)
      } catch (e: JwtTokenException) {
        logger.warn("Invalid token: {}", e.message)
        throw BadRequestException("登录态无效，请重新登录")
      }

    if (userPrincipal == null) {
      throw BadRequestException("登录态无效，请重新登录")
    }

    // Check if token is expired
    if (jwtTokenService.isTokenExpired(actualToken)) {
      throw BadRequestException("登录态已过期，请重新登录")
    }

    // Query worker info by userId
    val admin =
      adminRepository.findById(userPrincipal.id).orElse(null)
        ?: throw BadRequestException("用户不存在，请重新登录")

    return DeliveryLoginResponse(
      token = actualToken,
      refreshToken = null,
      needBindPhone = false,
      workerInfo = admin.deliveryWorkerId?.let { getWorkInfo(it) },
      message = "登录态有效",
    )
  }
}
