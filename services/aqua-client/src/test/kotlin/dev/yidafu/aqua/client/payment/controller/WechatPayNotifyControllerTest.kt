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

package dev.yidafu.aqua.client.payment.controller

import dev.yidafu.aqua.api.service.payment.PaymentService
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * 微信支付回调端点测试(PRD 04-order.md §F-ORD-005/R-PAY-01):
 * 原始报文+验签头透传;处理成功返回 SUCCESS,失败返回 500 FAIL(微信重试)。
 */
class WechatPayNotifyControllerTest :
  FunSpec({
    var paymentService: PaymentService = mockk()
    var mockMvc: MockMvc = mockk()

    beforeTest {
      paymentService = mockk()
      mockMvc = MockMvcBuilders.standaloneSetup(WechatPayNotifyController(paymentService)).build()
    }
    test("notifySuccessReturnsSuccess") {
      every { paymentService.handleWechatPayCallback(any(), any()) } returns true

      mockMvc
        .perform(
          post("/api/pay/notify")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"resource\":{\"out_trade_no\":\"ORD-1\"}}")
            .header("wechatpay-serial", "SERIAL-1")
            .header("wechatpay-timestamp", "1750000000")
            .header("wechatpay-nonce", "nonce-1")
            .header("wechatpay-signature", "sig-1"),
        ).andExpect(status().isOk)
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
    }
    test("notifyFailureReturnsFail") {
      every { paymentService.handleWechatPayCallback(any(), any()) } returns false

      mockMvc
        .perform(post("/api/pay/notify").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isInternalServerError)
        .andExpect(jsonPath("$.code").value("FAIL"))
    }
    test("notifyExceptionReturnsFail") {
      every { paymentService.handleWechatPayCallback(any(), any()) } throws IllegalStateException("boom")

      mockMvc
        .perform(post("/api/pay/notify").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isInternalServerError)
        .andExpect(jsonPath("$.code").value("FAIL"))
    }
  })
