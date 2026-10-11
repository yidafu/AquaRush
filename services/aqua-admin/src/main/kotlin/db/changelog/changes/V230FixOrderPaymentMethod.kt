// 转换自 changes/v2.3.0-fix-order-payment-method.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V230FixOrderPaymentMethod :
  KotlinCompiledDatabaseChangeLog({
    // aqua_orders.payment_method 枚举对齐:
    //     南山演示数据(v1.7.0-seed-nanshan-demo-data.xml)将 2 笔订单的 payment_method
    //     写成 'CASH_ON_DELIVERY',该值不在 JPA 域枚举 PaymentMethod(WECHAT_PAY/CASH,
    //     见 modules/aqua-foundation .../PaymentModel.kt)中 —— 域口径见 spec/prd/04-order.md
    //     §R-PAY-03:在线支付 WECHAT_PAY,现场收款/代客下单 CASH。
    //     后果:orderStatistics 服务层经 orderRepository.findAll() 加载订单实体时,
    //     Hibernate 枚举映射抛 IllegalArgumentException(No enum constant
    //     PaymentMethod.CASH_ON_DELIVERY) → GraphQL INTERNAL_ERROR。
    //     修正:归一为 CASH(货到付款/现场收款);GraphQL 层 PaymentMethod 枚举
    //     (shared-types.graphqls) 的 CASH_ON_DELIVERY 仅用于展示映射,无服务端消费。
    //     幂等 UPDATE,已执行环境无副作用(0 行受影响)。
    changeSet(author = "aqua-rush", id = "2.3.0-fix-order-payment-method") {
      comment("Normalize aqua_orders.payment_method CASH_ON_DELIVERY to CASH (domain enum PaymentMethod)")
      update(tableName = "aqua_orders") {
        column(name = "payment_method", value = "CASH")
        where("payment_method = 'CASH_ON_DELIVERY'")
      }
    }
  })
