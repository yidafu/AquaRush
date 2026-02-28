/*
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

package dev.yidafu.aqua.delivery.service.impl

import cn.binarywang.wx.miniapp.api.WxMaService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.delivery.service.DeliveryAuthService
import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginRequest
import dev.yidafu.aqua.delivery.service.dto.DeliveryLoginResponse
import dev.yidafu.aqua.delivery.service.dto.toDeliveryWorkerInfo
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import me.chanjar.weixin.common.error.WxErrorException
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryAuthServiceImpl(
  private val wxMaService: WxMaService,
  private val jwtTokenService: JwtTokenService,
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val adminRepository: AdminRepository
) : DeliveryAuthService {
  private val logger = LoggerFactory.getLogger(DeliveryAuthServiceImpl::class.java)

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
          workerInfo = existingWorker.toDeliveryWorkerInfo(),
          message = "登录成功",
          openId = openId
        )
      }

      // Worker not bound, need to bind phone
      return DeliveryLoginResponse(
        token = null,
        refreshToken = null,
        needBindPhone = true,
        workerInfo = null,
        message = "请绑定手机号",
        openId = openId
      )
    } catch (e: Exception) {
      logger.error("Delivery worker login failed", e)
      throw BadRequestException("登录失败: ${e.message}")
    }
  }

  @Transactional
  override fun bindPhone(openId: String, phoneNumber: String): DeliveryLoginResponse {
    // Verify phone number belongs to admin or delivery worker
    val isAdmin = adminRepository.existsByPhone(phoneNumber)
    val isDeliveryWorker = deliveryWorkerRepository.existsByPhone(phoneNumber)

    if (!isAdmin && !isDeliveryWorker) {
      throw BadRequestException("该手机号未注册为管理员或送水员，请联系管理员")
    }

    // Check if worker already exists
    var worker = deliveryWorkerRepository.findByWechatOpenId(openId)
    if (worker != null) {
      // Update existing worker with new phone
      worker.phone = phoneNumber
      worker = deliveryWorkerRepository.save(worker)
      logger.info("Updated existing worker: id={}, phone={}", worker.id, phoneNumber)
    } else {
      // Create new delivery worker record
      worker = DeliveryWorkerModel(
        wechatOpenId = openId,
        name = "送水员",
        phone = phoneNumber,
        onlineStatus = DeliverWorkerModelStatus.OFFLINE,
        isAvailable = true
      )
      worker = deliveryWorkerRepository.save(worker)
      logger.info("Created new delivery worker: id={}, phone={}", worker.id, phoneNumber)
    }

    // Generate token
    val token = generateToken(worker)
    return DeliveryLoginResponse(
      token = token,
      refreshToken = null,
      needBindPhone = false,
      workerInfo = worker.toDeliveryWorkerInfo(),
      message = "绑定成功"
    )
  }

  /**
   * Exchange WeChat code for OpenID
   */
  private fun exchangeCodeForOpenId(code: String): String {
    return try {
      val session = wxMaService.userService.getSessionInfo(code)
      session.openid ?: throw IllegalStateException("OpenID is null")
    } catch (e: WxErrorException) {
      logger.error("WeChat code exchange failed", e)
      throw BadRequestException("微信登录失败: ${e.message}")
    }
  }

  /**
   * Generate JWT token for delivery worker
   */
  private fun generateToken(worker: DeliveryWorkerModel): String {
    val authorities = listOf(SimpleGrantedAuthority("ROLE_DELIVERY_WORKER"))
    val userPrincipal = UserPrincipal(
      id = worker.id!!,
      _username = worker.wechatOpenId,
      userType = "DELIVERY_WORKER",
      _authorities = authorities
    )
    return jwtTokenService.generateAccessToken(userPrincipal)
  }
}
