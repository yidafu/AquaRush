// 转换自 changes/v1.6.0-convert-address-coords-to-wgs84.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V160ConvertAddressCoordsToWgs84 :
  KotlinCompiledDatabaseChangeLog({
    // 坐标体系统一(PRD 05-address-delivery.md §坐标体系):
    //     自 v1.6.0 起存储/接口坐标统一为 WGS-84(与 MapLibre / 天地图底图一致),
    //     存量 aqua_addresses / delivery_addresses 的 GCJ-02 坐标一次性转换为 WGS-84;
    //     算法与 frontend/common/src/utils/coordinate.ts gcj02ToWgs84 一致(单步逆变换,残差 ~1m)。
    changeSet(author = "aqua-rush", id = "1.6.0-convert-address-coords-to-wgs84") {
      comment("Convert legacy GCJ-02 address coordinates to WGS-84 (MapLibre/Tianditu consistent)")
      sqlFile(path = "db/changelog/changes/sql/1.6.0-address-coords-gcj-to-wgs84.sql")
    }
  })
