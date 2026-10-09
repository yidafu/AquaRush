// 转换自 changes/v1.0.9-add-marketing-tables.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
// 存量漂移修复:冗余 addColumn(已被就地并入 v1.0.0)已移除,原因:users.customer_group_id 及 orders 营销快照列已被就地并入 v1.0.0
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V109AddMarketingTables :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.9-add-marketing-tables") {
      comment(
        "Add marketing domain tables (customer groups, group prices, coupons, promotion activities, water tickets) and order price snapshots for F-MKT-001/002/003/004/007 (PRD 10-marketing.md)",
      )
      // 客户分组表(F-MKT-001)
      createTable(tableName = "customer_groups") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "description", type = "varchar(500)")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "customer_groups", indexName = "uk_customer_groups_name", unique = true) {
        column(name = "name")
      }
      // 分组商品专属价表(F-MKT-002,无行=回退目录价)
      createTable(tableName = "group_product_prices") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "customer_group_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "group_product_prices", indexName = "uk_gpp_group_product", unique = true) {
        column(name = "customer_group_id")
        column(name = "product_id")
      }
      createIndex(tableName = "group_product_prices", indexName = "idx_gpp_product") {
        column(name = "product_id")
      }
      // 优惠券模板表(F-MKT-003)
      createTable(tableName = "coupon_templates") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "coupon_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "threshold_cents", type = "bigint")
        column(name = "discount_amount_cents", type = "bigint")
        column(name = "discount_rate_permille", type = "bigint")
        column(name = "scope", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "validity_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "valid_from", type = "timestamp")
        column(name = "valid_to", type = "timestamp")
        column(name = "valid_days", type = "int")
        column(name = "issuance_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "per_user_limit", type = "int", defaultValueNumeric = 1) {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      // 用户优惠券实例表(参数快照,配置变更不影响已领券)
      createTable(tableName = "user_coupons") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "coupon_template_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "coupon_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "threshold_cents", type = "bigint")
        column(name = "discount_amount_cents", type = "bigint")
        column(name = "discount_rate_permille", type = "bigint")
        column(name = "scope", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "expire_time", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "claim_time", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "used_order_id", type = "bigint")
        column(name = "used_at", type = "timestamp")
        column(name = "returned_at", type = "timestamp")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "user_coupons", indexName = "idx_user_coupons_user_status") {
        column(name = "user_id")
        column(name = "status")
      }
      createIndex(tableName = "user_coupons", indexName = "idx_user_coupons_used_order") {
        column(name = "used_order_id")
      }
      // 满减/折扣活动表(F-MKT-004)
      createTable(tableName = "promotion_activities") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "activity_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "scope", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "customer_group_id", type = "bigint")
        column(name = "valid_from", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "valid_to", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      // 活动档位表(档位金额递增且互斥,取最高已达标档)
      createTable(tableName = "promotion_activity_tiers") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "activity_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "threshold_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "discount_amount_cents", type = "bigint")
        column(name = "discount_rate_permille", type = "bigint")
        column(name = "sort", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "promotion_activity_tiers", indexName = "idx_pat_activity_sort") {
        column(name = "activity_id")
        column(name = "sort")
      }
      // 桶水票票种表(F-MKT-007)
      createTable(tableName = "ticket_types") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "ticket_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "specification", type = "varchar(100)")
        column(name = "package_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "package_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "gift_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "validity_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "valid_from", type = "timestamp")
        column(name = "valid_to", type = "timestamp")
        column(name = "valid_days", type = "int")
        column(name = "per_user_limit", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      // 用户水票实例表(每票一行,参数快照)
      createTable(tableName = "user_tickets") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "ticket_type_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "purchase_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "expire_time", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "is_gift", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "used_order_id", type = "bigint")
        column(name = "used_at", type = "timestamp")
        column(name = "returned_at", type = "timestamp")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "user_tickets", indexName = "idx_user_tickets_user_status") {
        column(name = "user_id")
        column(name = "status")
      }
      createIndex(tableName = "user_tickets", indexName = "idx_user_tickets_used_order") {
        column(name = "used_order_id")
      }
      // 购票单表(购票即预收款项,不支持退款;order_no 作为支付 out_trade_no)
      createTable(tableName = "ticket_purchases") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "ticket_type_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_no", type = "varchar(64)") {
          constraints(nullable = false)
        }
        column(name = "package_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "gift_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "total_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "package_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "payment_status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "transaction_id", type = "varchar(64)")
        column(name = "paid_at", type = "timestamp")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "ticket_purchases", indexName = "uk_ticket_purchases_order_no", unique = true) {
        column(name = "order_no")
      }
      createIndex(tableName = "ticket_purchases", indexName = "idx_ticket_purchases_user_status") {
        column(name = "user_id")
        column(name = "payment_status")
      }
    }
  })
