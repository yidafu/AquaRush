package dev.yidafu.aqua.delivery.service.impl

import cn.binarywang.wx.miniapp.api.WxMaService
import dev.yidafu.aqua.api.dto.DeliveryLoginRequest
import dev.yidafu.aqua.api.dto.DeliveryLoginResponse
import dev.yidafu.aqua.api.dto.DeliveryWorkerInfo
import dev.yidafu.aqua.api.service.DeliveryAuthService
import dev.yidafu.aqua.common.domain.model.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.JwtTokenException
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.ext.toSimpleGrantedAuthority
import dev.yidafu.aqua.user.service.dto.toDeliveryWorkerInfo
import me.chanjar.weixin.common.error.WxErrorException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryAuthServiceImpl(
  private val wxMaService: WxMaService,
  private val jwtTokenService: JwtTokenService,
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val adminRepository: AdminRepository,
  private val userRepository: UserRepository,
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
      wechatOpenId = worker.wechatOpenId,
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
        val token = generateToken(existingWorker)
        return DeliveryLoginResponse(
          token = token,
          refreshToken = null,
          needBindPhone = false,
          workerInfo = getWorkInfo(existingWorker.id!!),
          message = "登录成功",
          openId = openId,
        )
      }

      // Worker not bound, need to bind phone
      return DeliveryLoginResponse(
        token = null,
        refreshToken = null,
        needBindPhone = true,
        workerInfo = null,
        message = "请绑定手机号",
        openId = openId,
      )
    } catch (e: Exception) {
      logger.error("Delivery worker login failed", e)
      throw BadRequestException("登录失败: ${e.message}")
    }
  }

  @Transactional
  override fun bindPhone(
    openId: String,
    phoneNumber: String,
  ): DeliveryLoginResponse {
    // Verify phone number belongs to admin or delivery worker
    val isAdmin = adminRepository.existsByPhone(phoneNumber)

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

    // 已有该 openId 的送水员，绑定用户ID
    worker.userId = user.id
    worker.phone = phoneNumber
    worker = deliveryWorkerRepository.save(worker)
    logger.info(
      "Updated existing worker: id={}, phone={}, openId={}, userId={}",
      worker.id,
      phoneNumber,
      openId,
      user.id,
    )

    // Generate token
    val token = generateToken(worker)
    return DeliveryLoginResponse(
      token = token,
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
  private fun generateToken(worker: DeliveryWorkerModel): String {
    val authorities = listOf(AdminRoleModel.DELIVERY_WORKER.toSimpleGrantedAuthority())
    val userPrincipal =
      UserPrincipal(
        id = worker.id!!,
        _username = worker.wechatOpenId,
        userType = "DELIVERY_WORKER",
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
    val worker =
      deliveryWorkerRepository.findById(userPrincipal.id).orElse(null)
        ?: throw BadRequestException("用户不存在，请重新登录")

    return DeliveryLoginResponse(
      token = actualToken,
      refreshToken = null,
      needBindPhone = false,
      workerInfo = getWorkInfo(worker.id!!),
      message = "登录态有效",
    )
  }
}
