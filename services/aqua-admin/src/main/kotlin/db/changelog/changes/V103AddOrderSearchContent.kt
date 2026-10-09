// 转换自 changes/v1.0.3-add-order-search-content.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V103AddOrderSearchContent :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.3-add-order-search-content") {
      // 修复存量漂移(该缺陷在 XML 时代就存在,全新库上必失败):orders.search_content 与 idx_orders_search_content 已被就地并入 v1.0.0 建表
      preConditions(onFail = "MARK_RAN", onErrorMessage = "search_content 已存在,跳过") {
        not {
          columnExists(tableName = "orders", columnName = "search_content")
        }
      }
      comment("Add search_content field for full-text search on orders")
      // Add search_content column as text
      addColumn(tableName = "orders") {
        column(name = "search_content", type = "text")
      }
      // Create GIN index for full-text search
      createIndex(tableName = "orders", indexName = "idx_orders_search_content") {
        column(name = "search_content")
      }
    }
  })
