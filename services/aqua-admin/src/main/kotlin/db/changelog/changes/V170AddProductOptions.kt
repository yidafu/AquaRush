// 转换自 changes/v1.7.0-add-product-options.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V170AddProductOptions :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.7.0-add-product-options") {
      comment("Add product option dictionary table (specification / water source) for F-PRO-009")
      // 商品属性选项字典:规格 / 水源地下拉选项的后台可配置来源(替换前端硬编码候选)
      createTable(tableName = "product_options") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "option_type", type = "varchar(32)") {
          constraints(nullable = false)
        }
        column(name = "option_value", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "sort_order", type = "int", defaultValueNumeric = 999) {
          constraints(nullable = false)
        }
        column(name = "enabled", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "product_options", indexName = "idx_product_options_type_deleted") {
        column(name = "option_type")
        column(name = "is_deleted")
      }
      createIndex(tableName = "product_options", indexName = "idx_product_options_type_sort") {
        column(name = "option_type")
        column(name = "sort_order")
      }
      // 种子数据:规格(承接原前端硬编码候选,可后台增改)
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793601")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "330ml")
        column(name = "sort_order", value = "1")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793602")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "500ml")
        column(name = "sort_order", value = "2")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793603")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "600ml")
        column(name = "sort_order", value = "3")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793604")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "750ml")
        column(name = "sort_order", value = "4")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793605")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "1L")
        column(name = "sort_order", value = "5")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793606")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "1.5L")
        column(name = "sort_order", value = "6")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793607")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "2L")
        column(name = "sort_order", value = "7")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793608")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "4L")
        column(name = "sort_order", value = "8")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793609")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "5L")
        column(name = "sort_order", value = "9")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793610")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "10L")
        column(name = "sort_order", value = "10")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793611")
        column(name = "option_type", value = "SPECIFICATION")
        column(name = "option_value", value = "19L")
        column(name = "sort_order", value = "11")
        column(name = "enabled", valueBoolean = true)
      }
      // 种子数据:水源地(承接原前端硬编码候选,可后台增改)
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793701")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "长白山")
        column(name = "sort_order", value = "1")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793702")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "昆仑山")
        column(name = "sort_order", value = "2")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793703")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "农夫山泉")
        column(name = "sort_order", value = "3")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793704")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "依云")
        column(name = "sort_order", value = "4")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793705")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "巴黎水")
        column(name = "sort_order", value = "5")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793706")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "圣培露")
        column(name = "sort_order", value = "6")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793707")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "斐济水")
        column(name = "sort_order", value = "7")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793708")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "VOSS")
        column(name = "sort_order", value = "8")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793709")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "西藏5100")
        column(name = "sort_order", value = "9")
        column(name = "enabled", valueBoolean = true)
      }
      insert(tableName = "product_options") {
        column(name = "id", value = "7205759403792793710")
        column(name = "option_type", value = "WATER_SOURCE")
        column(name = "option_value", value = "恒大冰泉")
        column(name = "sort_order", value = "10")
        column(name = "enabled", valueBoolean = true)
      }
    }
  })
