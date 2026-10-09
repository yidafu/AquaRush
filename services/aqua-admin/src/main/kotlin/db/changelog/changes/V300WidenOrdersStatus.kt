// JPA→Exposed 迁移 P4 新增(changeset 非转换,无 XML 前身)。
// logicalFilePath 指向假想的 v3.0.0 XML —— 与 Kotlin DSL 其余 changeset 惯例一致,
// 仅作 changeset 身份路径,文件不存在不影响运行期(编译期 changelog 按类 FQCN 解析)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V300WidenOrdersStatus :
  KotlinCompiledDatabaseChangeLog({
    // orders.status 初建为 varchar(20)(V100),但域枚举 OrderModelStatus 的
    // AWAITING_CONFIRMATION(R-ORD-11 待确认收货)为 21 字符:
    // - 生产 PG:写入直接 value too long 报错;
    // - Exposed:客户端按表定义列宽强校验,save/update 一律拒绝。
    // 加宽到 30(与 foundation SourceTables.OrdersTable.status 同步),存量数据无损。
    changeSet(author = "aqua-rush", id = "3.0.0-widen-orders-status") {
      comment("Widen orders.status varchar(20) -> varchar(30) for AWAITING_CONFIRMATION (R-ORD-11)")
      sql(
        sql = """
        ALTER TABLE orders ALTER COLUMN status TYPE varchar(30);

        """,
      )
    }
  })
