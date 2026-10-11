// B2 配送费分档(裁决结构 #6,PRD R-MON-02「2026-10 修订」楼层分档,弃统一一次性加价):
// aqua_delivery_fee_rules 增 tiers JSON 档位列;存量行回填 PRD 默认三档
// (免费楼层以上 2 层内 +2 元 / 再上 3 层 +5 元 / 更高 +8 元,免费楼层默认 4),
// free_floor_limit 保持原值;surcharge_cents 列保留为 tiers 缺失时的计价兜底。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V345AddDeliveryFeeTiers :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.5-add-delivery-fee-tiers") {
      comment("Backfill aqua_delivery_fee_rules.tiers with PRD default tiers (column 已并入 v1.0.7 建表)")
      sql(
        sql =
          "UPDATE aqua_delivery_fee_rules SET tiers = " +
            "'[{\"fromFloor\":5,\"toFloor\":6,\"feeCents\":200}," +
            "{\"fromFloor\":7,\"toFloor\":9,\"feeCents\":500}," +
            "{\"fromFloor\":10,\"toFloor\":null,\"feeCents\":800}]'::json " +
            "WHERE tiers IS NULL AND is_deleted = false",
      )
    }
  })
