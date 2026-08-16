-- ============================================================================
-- 地址坐标迁移:GCJ-02(火星坐标)→ WGS-84(与 MapLibre / 天地图底图一致)
-- 关联:PRD/05-address-delivery.md(坐标体系:存储/接口统一 WGS-84)
--
-- 背景:
-- - 存量 addresses / delivery_addresses 的经纬度由微信小程序地图选点(GCJ-02)
--   与管理后台旧腾讯地图选点(GCJ-02)写入;
-- - 自 v1.6.0 起所有写入方(小程序/管理后台/配送端)统一提交 WGS-84,
--   本迁移一次性把存量 GCJ-02 数据转换为 WGS-84(单步逆变换,残差 ~1m);
-- - 送水工 GPS 轨迹(delivery_trajectories)本就是 WGS-84,无需转换。
--
-- 算法与 frontend/common/src/utils/coordinate.ts gcj02ToWgs84 一致:
--   dLat/dLng 在输入(GCJ)点计算,然后 wgs = gcj - delta。
-- ============================================================================

-- ---------- addresses ----------
UPDATE addresses
SET longitude = longitude - (
      300.0 + (longitude - 105.0) + 2.0 * (latitude - 35.0)
      + 0.1 * (longitude - 105.0) * (longitude - 105.0)
      + 0.1 * (longitude - 105.0) * (latitude - 35.0)
      + 0.1 * sqrt(abs(longitude - 105.0))
      + ((20.0 * sin(6.0 * (longitude - 105.0) * pi()) + 20.0 * sin(2.0 * (longitude - 105.0) * pi())) * 2.0 / 3.0)
      + ((20.0 * sin((longitude - 105.0) * pi()) + 40.0 * sin((longitude - 105.0) / 3.0 * pi())) * 2.0 / 3.0)
      + ((150.0 * sin((longitude - 105.0) / 12.0 * pi()) + 300.0 * sin((longitude - 105.0) / 30.0 * pi())) * 2.0 / 3.0)
    ) * 180.0 / (
      6378245.0 / sqrt(1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0))
      * cos(latitude * pi() / 180.0) * pi()
    ),
  latitude = latitude - (
      -100.0 + 2.0 * (longitude - 105.0) + 3.0 * (latitude - 35.0)
      + 0.2 * (latitude - 35.0) * (latitude - 35.0)
      + 0.1 * (longitude - 105.0) * (latitude - 35.0)
      + 0.2 * sqrt(abs(longitude - 105.0))
      + ((20.0 * sin(6.0 * (longitude - 105.0) * pi()) + 20.0 * sin(2.0 * (longitude - 105.0) * pi())) * 2.0 / 3.0)
      + ((20.0 * sin((latitude - 35.0) * pi()) + 40.0 * sin((latitude - 35.0) / 3.0 * pi())) * 2.0 / 3.0)
      + ((160.0 * sin((latitude - 35.0) / 12.0 * pi()) + 320.0 * sin((latitude - 35.0) * pi() / 30.0)) * 2.0 / 3.0)
    ) * 180.0 / (
      (6378245.0 * (1.0 - 0.00669342162296594323))
      / ((1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0))
         * sqrt(1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0)))
      * pi()
    )
WHERE longitude IS NOT NULL
  AND latitude IS NOT NULL
  AND longitude >= 72.004 AND longitude <= 137.8347
  AND latitude >= 0.8293 AND latitude <= 55.8271;

-- ---------- delivery_addresses(下单时的地址快照,同源 GCJ-02)----------
UPDATE delivery_addresses
SET longitude = longitude - (
      300.0 + (longitude - 105.0) + 2.0 * (latitude - 35.0)
      + 0.1 * (longitude - 105.0) * (longitude - 105.0)
      + 0.1 * (longitude - 105.0) * (latitude - 35.0)
      + 0.1 * sqrt(abs(longitude - 105.0))
      + ((20.0 * sin(6.0 * (longitude - 105.0) * pi()) + 20.0 * sin(2.0 * (longitude - 105.0) * pi())) * 2.0 / 3.0)
      + ((20.0 * sin((longitude - 105.0) * pi()) + 40.0 * sin((longitude - 105.0) / 3.0 * pi())) * 2.0 / 3.0)
      + ((150.0 * sin((longitude - 105.0) / 12.0 * pi()) + 300.0 * sin((longitude - 105.0) / 30.0 * pi())) * 2.0 / 3.0)
    ) * 180.0 / (
      6378245.0 / sqrt(1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0))
      * cos(latitude * pi() / 180.0) * pi()
    ),
  latitude = latitude - (
      -100.0 + 2.0 * (longitude - 105.0) + 3.0 * (latitude - 35.0)
      + 0.2 * (latitude - 35.0) * (latitude - 35.0)
      + 0.1 * (longitude - 105.0) * (latitude - 35.0)
      + 0.2 * sqrt(abs(longitude - 105.0))
      + ((20.0 * sin(6.0 * (longitude - 105.0) * pi()) + 20.0 * sin(2.0 * (longitude - 105.0) * pi())) * 2.0 / 3.0)
      + ((20.0 * sin((latitude - 35.0) * pi()) + 40.0 * sin((latitude - 35.0) / 3.0 * pi())) * 2.0 / 3.0)
      + ((160.0 * sin((latitude - 35.0) / 12.0 * pi()) + 320.0 * sin((latitude - 35.0) * pi() / 30.0)) * 2.0 / 3.0)
    ) * 180.0 / (
      (6378245.0 * (1.0 - 0.00669342162296594323))
      / ((1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0))
         * sqrt(1.0 - 0.00669342162296594323 * sin(latitude * pi() / 180.0) * sin(latitude * pi() / 180.0)))
      * pi()
    )
WHERE longitude IS NOT NULL
  AND latitude IS NOT NULL
  AND longitude >= 72.004 AND longitude <= 137.8347
  AND latitude >= 0.8293 AND latitude <= 55.8271;
