// 转换自 changes/v1.2.0-drop-order-single-product-columns.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V120DropOrderSingleProductColumns :
  KotlinCompiledDatabaseChangeLog({
    // 多商品下单(F-ORD-004,PRD 04-order.md):
    //     orders 删除单商品冗余列 product_id/quantity,订单商品权威数据以 order_items 表为准
    //     (order_items 建于 v1.0.0,changeSet 1.0.2-create-order-items-table,无 v1.0.1 文件)。
    changeSet(author = "aqua-rush", id = "1.2.0-drop-order-single-product-columns") {
      // preConditions 必须位于 changeSet 子元素首位(comment 之前),见 dbchangelog-4.33.xsd changeSet 内容模型
      preConditions(onFail = "CONTINUE", onErrorMessage = "orders.product_id/quantity 已不存在,跳过") {
        and {
          columnExists(tableName = "orders", columnName = "product_id")

          columnExists(tableName = "orders", columnName = "quantity")
        }
      }
      comment("Drop single-product columns from orders (multi-product F-ORD-004)")
      dropColumn(tableName = "orders") {
        column(name = "product_id")
        column(name = "quantity")
      }
    }
  })
