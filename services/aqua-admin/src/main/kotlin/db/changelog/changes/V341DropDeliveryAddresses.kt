// B1b 缺陷修复(裁决清单 §2.7):地址双表清理。
// delivery_addresses 为死表(V100 建,除 V160 坐标迁移外全仓无读写、无 Exposed 映射);
// orders.delivery_address_id 恒空列(模型/表定义外无任何读写方,订单地址为内联快照)。
// 删表 + 删列(V160 坐标迁移在其之前执行,历史时序不受影响;PG 随列自动删索引)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V341DropDeliveryAddresses :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.1-drop-delivery-addresses") {
      comment("Drop dead table delivery_addresses and empty column orders.delivery_address_id (defect #7)")
      sql(sql = "DROP TABLE IF EXISTS delivery_addresses")
      sql(sql = "ALTER TABLE orders DROP COLUMN IF EXISTS delivery_address_id")
    }
  })
