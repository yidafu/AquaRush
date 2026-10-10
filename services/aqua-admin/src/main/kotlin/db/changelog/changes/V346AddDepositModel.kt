// B2 押金模型回退(裁决结构 #4,PRD R-MON-03/R-PRO-04 恢复押金桶):
//     - deposit_records 扩列 specification/unit_deposit_cents/bucket_count(在租桶数按规格口径),
//       状态值域回退 PENDING(待收取)→HELD(已收取)→REFUNDED(已退还):
//       存量 COLLECTED→HELD、CANCELLED(未实际收取作废,无代码写点)→PENDING;
//     - 新建 deposit_refund_applications 退押申请(待处理→已回收→已退还/已驳回,R-PRO-04);
//     - 新建 barrel_recovery_records 空桶回收登记(送水工完成配送时按规格登记,F-ORD-011);
//     - orders 增 empty_barrels_retrieved 完成配送回收空桶总数快照(01 §4.1 目标列)。
// barrel_purchases/barrel_purchase_items(products.deposit_price_cents 同)保留历史数据不 drop;
// barrel_fees 表名保留、语义由一次性水桶费回退为押金价目(01 §3.4)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V346AddDepositModel :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.6-deposit-records-columns") {
      comment("Extend deposit_records with spec/unit/bucket_count and migrate status values (R-MON-03)")
      sql(sql = "ALTER TABLE deposit_records ADD COLUMN IF NOT EXISTS specification varchar(100)")
      sql(sql = "ALTER TABLE deposit_records ADD COLUMN IF NOT EXISTS unit_deposit_cents bigint")
      sql(sql = "ALTER TABLE deposit_records ADD COLUMN IF NOT EXISTS bucket_count int")
      sql(sql = "UPDATE deposit_records SET status = 'HELD' WHERE status = 'COLLECTED'")
      sql(
        sql = "UPDATE deposit_records SET status = 'PENDING' WHERE status = 'CANCELLED'",
      )
    }
    changeSet(author = "aqua-rush", id = "3.4.6-deposit-records-backfill-spec") {
      comment(
        "Backfill deposit_records spec/unit/bucket_count from single-spec order_items " +
          "(product_snapshot.specification;multi-spec legacy rows stay NULL)",
      )
      sql(
        sql =
          "UPDATE deposit_records d SET specification = s.specification, " +
            "bucket_count = s.quantity, " +
            "unit_deposit_cents = CASE WHEN s.quantity > 0 " +
            "THEN d.deposit_amount_cents / s.quantity ELSE d.deposit_amount_cents END " +
            "FROM (SELECT oi.order_id, MIN(oi.product_snapshot->>'specification') AS specification, " +
            "SUM(oi.quantity) AS quantity, " +
            "COUNT(DISTINCT oi.product_snapshot->>'specification') AS spec_cnt " +
            "FROM order_items oi GROUP BY oi.order_id) s " +
            "WHERE d.order_id = s.order_id AND d.specification IS NULL AND s.spec_cnt = 1",
      )
      sql(sql = "CREATE INDEX IF NOT EXISTS idx_deposit_user_spec ON deposit_records (user_id, specification, status)")
    }
    changeSet(author = "aqua-rush", id = "3.4.6-create-deposit-refund-applications") {
      comment("Create deposit_refund_applications table (R-PRO-04 refund application state machine)")
      sql(
        sql =
          "CREATE TABLE IF NOT EXISTS deposit_refund_applications (" +
            "id bigint PRIMARY KEY, " +
            "user_id bigint NOT NULL REFERENCES users(id), " +
            "specification varchar(100) NOT NULL, " +
            "bucket_count int NOT NULL, " +
            "deposit_cents bigint NOT NULL, " +
            "receiver_name varchar(50), " +
            "receiver_phone varchar(20), " +
            "receiver_detail varchar(500), " +
            "remark varchar(200), " +
            "status varchar(16) NOT NULL DEFAULT 'PENDING', " +
            "recovered_at timestamp, " +
            "refunded_at timestamp, " +
            "rejected_at timestamp, " +
            "handler_admin_id bigint, " +
            "reject_reason varchar(500), " +
            "is_deleted boolean NOT NULL DEFAULT false, " +
            "deleted_at timestamp, " +
            "deleted_by bigint, " +
            "created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")",
      )
      sql(
        sql = "CREATE INDEX IF NOT EXISTS idx_deposit_refund_user_status ON deposit_refund_applications (user_id, status)",
      )
      sql(sql = "CREATE INDEX IF NOT EXISTS idx_deposit_refund_status ON deposit_refund_applications (status)")
    }
    changeSet(author = "aqua-rush", id = "3.4.6-create-barrel-recovery-records") {
      comment("Create barrel_recovery_records table (F-ORD-011 empty barrel recovery registration)")
      sql(
        sql =
          "CREATE TABLE IF NOT EXISTS barrel_recovery_records (" +
            "id bigint PRIMARY KEY, " +
            "worker_id bigint NOT NULL REFERENCES admins(id), " +
            "order_id bigint NOT NULL REFERENCES orders(id), " +
            "specification varchar(100) NOT NULL, " +
            "bucket_count int NOT NULL, " +
            "is_deleted boolean NOT NULL DEFAULT false, " +
            "deleted_at timestamp, " +
            "deleted_by bigint, " +
            "created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")",
      )
      sql(sql = "CREATE INDEX IF NOT EXISTS idx_barrel_recovery_order ON barrel_recovery_records (order_id)")
      sql(sql = "CREATE INDEX IF NOT EXISTS idx_barrel_recovery_worker ON barrel_recovery_records (worker_id, created_at)")
    }
    changeSet(author = "aqua-rush", id = "3.4.6-add-orders-empty-barrels-retrieved") {
      comment("Add orders.empty_barrels_retrieved (denormalized total of barrel_recovery_records, 01 §4.1)")
      sql(sql = "ALTER TABLE orders ADD COLUMN IF NOT EXISTS empty_barrels_retrieved int")
    }
  })
