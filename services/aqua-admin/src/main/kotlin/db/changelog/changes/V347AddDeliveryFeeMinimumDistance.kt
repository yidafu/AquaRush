// G96 起送门槛与距离阶梯运费(PRD R-OPS-03,依调研报告 v2 F13/S4):
// delivery_fee_rules 增列 minimum_order_cents(起送门槛,分,0=不启用)、
// distance_tiers(距离阶梯档位 JSON:upToKm 距离上限 km / feeCents 加价分,与楼层分档叠加)、
// store_longitude/store_latitude(门店经纬度,距离计费基准;空=不启用距离计费)。
// 存量行默认 0/NULL 即「不启用」,行为与升级前一致;已生成订单不受影响(运费按下单时快照 R-MON-02)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V347AddDeliveryFeeMinimumDistance :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.7-add-delivery-fee-minimum-distance") {
      comment("Add R-OPS-03 minimum order / distance tiers / store coordinates to delivery_fee_rules")
      sql(sql = "ALTER TABLE delivery_fee_rules ADD COLUMN IF NOT EXISTS minimum_order_cents bigint NOT NULL DEFAULT 0")
      sql(sql = "ALTER TABLE delivery_fee_rules ADD COLUMN IF NOT EXISTS distance_tiers json")
      sql(sql = "ALTER TABLE delivery_fee_rules ADD COLUMN IF NOT EXISTS store_longitude double precision")
      sql(sql = "ALTER TABLE delivery_fee_rules ADD COLUMN IF NOT EXISTS store_latitude double precision")
    }
  })
