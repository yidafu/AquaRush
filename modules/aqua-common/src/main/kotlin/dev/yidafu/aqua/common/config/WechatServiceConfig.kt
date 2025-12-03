package dev.yidafu.aqua.common.config

import cn.binarywang.wx.miniapp.api.WxMaService
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class WechatServiceConfig {
  val logger = LoggerFactory.getLogger(WechatServiceConfig::class.java)

  @Value($$"${aqua.wechat.app-id}")
  var appId: String = ""

  @Value($$"${aqua.wechat.app-secret}")
  var appSecret: String = ""

  @Value($$"${aqua.wechat.token}")
  var token: String = ""

  @Value($$"${aqua.wechat.aes-key}")
  var aesKey: String = ""

  /**
   * 消息格式，XML或者JSON
   */
  private val msgDataFormat: String? = null

  @Bean
  fun wxMaService(): WxMaService {
    return WxMaServiceImpl().apply {
      wxMaConfig = WxMaDefaultConfigImpl().apply {
        appid = this@WechatServiceConfig.appId
        secret = this@WechatServiceConfig.appSecret
        token = this@WechatServiceConfig.token
        aesKey = this@WechatServiceConfig.aesKey
      }
    }
  }
}
