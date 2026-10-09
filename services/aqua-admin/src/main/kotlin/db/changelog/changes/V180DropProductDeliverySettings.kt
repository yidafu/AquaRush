// 转换自 changes/v1.8.0-drop-product-delivery-settings.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V180DropProductDeliverySettings :
  KotlinCompiledDatabaseChangeLog({
    // 商品级配送设置移除(PRD 03-product.md):
    //     配送配置统一由运营配置域提供(12-operations-config.md §F-OPS-005 配送费规则),
    //     products.delivery_settings 不再使用,删除该列(代码、GraphQL schema 同步移除)。
    changeSet(author = "aqua-rush", id = "1.8.0-drop-product-delivery-settings") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "products.delivery_settings 已不存在,跳过") {
        columnExists(tableName = "products", columnName = "delivery_settings")
      }
      comment("Drop delivery_settings column from products (unified delivery config F-OPS-005)")
      dropColumn(tableName = "products", columnName = "delivery_settings")
    }
  })
