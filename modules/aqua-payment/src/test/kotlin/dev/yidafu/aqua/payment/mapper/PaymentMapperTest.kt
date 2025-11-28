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

//package dev.yidafu.aqua.payment.mapper
//
//import dev.yidafu.aqua.api.dto.PaymentDTO
//import dev.yidafu.aqua.common.domain.model.Payment
//import dev.yidafu.aqua.common.domain.model.PaymentMethod
//import dev.yidafu.aqua.common.domain.model.PaymentStatus
//import dev.yidafu.aqua.common.mapper.MapperTestBase
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import java.math.BigDecimal
//import java.time.LocalDateTime
//
//class PaymentMapperTest : MapperTestBase() {
//
//    @Test
//    fun `should map Payment to PaymentDTO correctly`() {
//        // Given
//        val payment = Payment(
//            id = 1L,
//            orderId = 100L,
//            userId = 200L,
//            transactionId = "txn_123456",
//            prepayId = "prepay_789",
//            amount = BigDecimal("29.90"),
//            currency = "CNY",
//            status = PaymentStatus.SUCCESS,
//            paymentMethod = PaymentMethod.WECHAT_PAY,
//            description = "水费支付",
//            failureReason = null,
//            paidAt = LocalDateTime.of(2023, 12, 25, 10, 30, 0),
//            expiredAt = LocalDateTime.of(2023, 12, 25, 11, 30, 0),
//        )
//
//        // When
//        val result = PaymentMapper.map(payment)
//
//        // Then
//        verifyMappingCompleteness({ PaymentMapper.map(it) }, payment, PaymentDTO::class)
//
//        // 验证特定字段
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "id", 1L)
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "orderId", 100L)
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "amount", BigDecimal("29.90"))
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "status", PaymentStatus.SUCCESS)
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "paymentMethod", PaymentMethod.WECHAT_PAY)
//        verifyFieldMapping({ PaymentMapper.map(it) }, payment, "transactionId", "txn_123456")
//
//        // 验证敏感字段被排除
//        assertThat(result.userId).isNull()
//        assertThat(result.prepayId).isNull()
//        assertThat(result.currency).isNull()
//        assertThat(result.description).isNull()
//        assertThat(result.failureReason).isNull()
//        assertThat(result.expiredAt).isNull()
//    }
//}
