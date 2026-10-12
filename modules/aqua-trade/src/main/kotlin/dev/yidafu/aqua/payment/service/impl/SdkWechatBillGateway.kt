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

package dev.yidafu.aqua.payment.service.impl

import com.wechat.pay.java.core.RSAAutoCertificateConfig
import com.wechat.pay.java.core.exception.ServiceException
import com.wechat.pay.java.service.billdownload.BillDownloadServiceExtension
import com.wechat.pay.java.service.billdownload.model.BillType
import com.wechat.pay.java.service.billdownload.model.GetFundFlowBillRequest
import com.wechat.pay.java.service.billdownload.model.GetTradeBillRequest
import dev.yidafu.aqua.payment.service.WechatBillGateway
import dev.yidafu.aqua.payment.service.WechatBillType
import dev.yidafu.aqua.payment.service.WechatFundFlowBill
import dev.yidafu.aqua.payment.service.WechatFundFlowSummary
import dev.yidafu.aqua.payment.service.WechatTradeBill
import dev.yidafu.aqua.payment.service.WechatTradeBillSummary
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 微信对账单网关真实实现(基于 wechatpay-java 0.2.17 `BillDownloadServiceExtension`,设计 07 §4):
 *
 * - 申请账单(`GET /v3/bill/tradebill` / `GET /v3/bill/fundflowbill`)→ SDK 下载账单文件 →
 *   SHA1 哈希校验(`DigestBillEntity.verifyHash`,官方:下载响应无签名,以哈希保证完整性)→ CSV 解析;
 * - 凭据与 [SdkWechatPayGateway] 同源于 wechat.pay.*(商户号/商户私钥/证书序列号/APIv3 密钥),密钥不落库;
 * - 平台证书由 SDK 自动下载(RSAAutoCertificateConfig),惰性构建,测试环境假支付不受影响;
 * - 错误语义(官方「下载账单」错误码):
 *   - `NO_STATEMENT_EXIST` 当日无交易/资金变动 → 返回空账单(合法状态,非错误);
 *   - `STATEMENT_CREATING` 账单生成中 → 抛异常,上层置任务 FAILED(调度已安排在次日 10 点后);
 *   - 其余(SIGN_ERROR/PARAM_ERROR/FREQUENCY_LIMITED/SYSTEM_ERROR/HTTP 异常)→ 抛异常,失败不自动重试(设计 §7),
 *     由次日调度推进账单日或人工手动重跑。
 * - 不设 tar_type:单日账单体量小,明文流直接 UTF-8 读取,规避 GZIP 与哈希口径的歧义(官方默认即不压缩)。
 */
@Service
class SdkWechatBillGateway(
  @Value("\${wechat.pay.mchid:}") private val mchId: String,
  @Value("\${wechat.pay.private-key-path:}") private val privateKeyPath: String,
  @Value("\${wechat.pay.merchant-serial-number:}") private val merchantSerialNumber: String,
  @Value("\${wechat.pay.api-v3-key:}") private val apiV3Key: String,
) : WechatBillGateway {
  private val logger = LoggerFactory.getLogger(SdkWechatBillGateway::class.java)

  override val isConfigured: Boolean =
    mchId.isNotBlank() &&
      privateKeyPath.isNotBlank() &&
      merchantSerialNumber.isNotBlank() &&
      apiV3Key.isNotBlank()

  private val config: RSAAutoCertificateConfig by lazy {
    logger.info("初始化微信对账单 V3 配置(自动下载平台证书)...")
    RSAAutoCertificateConfig
      .Builder()
      .merchantId(mchId)
      .privateKeyFromPath(privateKeyPath)
      .merchantSerialNumber(merchantSerialNumber)
      .apiV3Key(apiV3Key)
      .build()
  }

  private val billService: BillDownloadServiceExtension by lazy {
    BillDownloadServiceExtension.Builder().config(config).build()
  }

  override fun downloadTradeBill(
    billDate: LocalDate,
    billType: WechatBillType,
  ): WechatTradeBill {
    ensureConfigured("交易账单")
    val request =
      GetTradeBillRequest().apply {
        this.billDate = billDate.toString()
        this.billType = billType.toSdk()
      }
    val content =
      downloadAndVerify({ billService.getTradeBill(request) }, "交易账单", billDate)
        ?: return WechatTradeBill(billDate, billType, emptyList(), WechatTradeBillSummary(0, emptyMap()))
    return WechatBillParser.parseTradeBill(content, billDate, billType)
  }

  override fun downloadFundFlowBill(billDate: LocalDate): WechatFundFlowBill {
    ensureConfigured("资金账单")
    val request =
      GetFundFlowBillRequest().apply {
        this.billDate = billDate.toString()
      }
    val content =
      downloadAndVerify({ billService.getFundFlowBill(request) }, "资金账单", billDate)
        ?: return WechatFundFlowBill(billDate, emptyList(), WechatFundFlowSummary(0, 0, 0, 0, 0))
    return WechatBillParser.parseFundFlowBill(content, billDate)
  }

  private fun ensureConfigured(label: String) {
    check(isConfigured) { "未配置微信支付商户参数(wechat.pay.*),无法下载$label" }
  }

  /** 账单类型 → SDK 官方枚举(仓库守卫:main 源码禁止跨枚举 valueOf,用显式映射) */
  private fun WechatBillType.toSdk(): BillType =
    when (this) {
      WechatBillType.ALL -> BillType.ALL
      WechatBillType.SUCCESS -> BillType.SUCCESS
      WechatBillType.REFUND -> BillType.REFUND
    }

  /**
   * 申请 + 下载 + SHA1 校验,返回账单明文(UTF-8)。
   * 当日无账单(NO_STATEMENT_EXIST)返回 null(合法空状态);其余错误抛异常。
   * 注意:必须先完整读取流(DigestInputStream 边读边累积摘要)再校验哈希,校验失败即抛、不解析未校验数据。
   */
  private fun downloadAndVerify(
    apply: () -> com.wechat.pay.java.service.billdownload.DigestBillEntity,
    label: String,
    billDate: LocalDate,
  ): String? =
    try {
      val entity = apply()
      val bytes = entity.inputStream.use { it.readBytes() }
      check(entity.verifyHash()) { "$label SHA1 哈希校验失败(billDate=$billDate),账单文件完整性不可信" }
      logger.info("{}下载并校验成功: billDate={} bytes={}", label, billDate, bytes.size)
      String(bytes, Charsets.UTF_8)
    } catch (e: ServiceException) {
      when (e.errorCode) {
        "NO_STATEMENT_EXIST" -> {
          logger.info("{}不存在(当日无记录),按空账单处理: billDate={}", label, billDate)
          null
        }
        "STATEMENT_CREATING" ->
          throw IllegalStateException("${label}生成中(STATEMENT_CREATING),请于次日10点后重试(billDate=$billDate)", e)
        else ->
          throw IllegalStateException(
            "${label}申请失败(code=${e.errorCode ?: e.httpStatusCode}, message=${e.errorMessage}, billDate=$billDate)",
            e,
          )
      }
    }
}
