/**
 * 坐标系转换工具(GCJ-02 ↔ WGS-84)
 *
 * 背景(与 PRD/05-address-delivery.md 及地图迁移保持一致):
 * - **存储/接口坐标统一为 WGS-84**(与 MapLibre、天地图底图一致);
 * - 存量地址坐标由微信小程序地图选点/腾讯地图选点写入,为 **GCJ-02(火星坐标)**,
 *   已由后端迁移 v1.6.0 一次性转换为 WGS-84;
 * - 微信小程序内部使用微信地图(GCJ-02):写入前需 `gcj02ToWgs84`,
 *   展示后端返回的 WGS-84 坐标前需 `wgs84ToGcj02`;
 * - 管理后台腾讯地理编码返回 GCJ-02,入库前需 `gcj02ToWgs84`;
 *   逆地理编码请求(腾讯 API 需 GCJ-02)前需 `wgs84ToGcj02`;
 * - 送水工 GPS 轨迹(react-native-fitness-geolocation)本就是 WGS-84,直接使用。
 *
 * 算法为标准 GCJ-02 偏移算法(eviltransform,纯数学计算、无网络请求)。
 */

const A = 6378245.0 // 克拉索夫斯基椭球长半轴
const EE = 0.00669342162296594323 // 偏心率平方

/** 判断坐标是否在中国境外(境外 GCJ-02 与 WGS-84 无差异) */
export function isOutOfChina(lng: number, lat: number): boolean {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(x: number, y: number): number {
  let ret =
    -100.0 +
    2.0 * x +
    3.0 * y +
    0.2 * y * y +
    0.1 * x * y +
    0.2 * Math.sqrt(Math.abs(x))
  ret +=
    ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) *
      2.0) /
    3.0
  ret +=
    ((20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin((y / 3.0) * Math.PI)) *
      2.0) /
    3.0
  ret +=
    ((160.0 * Math.sin((y / 12.0) * Math.PI) +
      320.0 * Math.sin((y * Math.PI) / 30.0)) *
      2.0) /
    3.0
  return ret
}

function transformLng(x: number, y: number): number {
  let ret =
    300.0 +
    x +
    2.0 * y +
    0.1 * x * x +
    0.1 * x * y +
    0.1 * Math.sqrt(Math.abs(x))
  ret +=
    ((20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) *
      2.0) /
    3.0
  ret +=
    ((20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin((x / 3.0) * Math.PI)) *
      2.0) /
    3.0
  ret +=
    ((150.0 * Math.sin((x / 12.0) * Math.PI) +
      300.0 * Math.sin((x / 30.0) * Math.PI)) *
      2.0) /
    3.0
  return ret
}

/** 计算 GCJ-02 相对 WGS-84 的偏移量(经纬度,单位度) */
function delta(lng: number, lat: number): { lat: number; lng: number } {
  const dLat = transformLat(lng - 105.0, lat - 35.0)
  const dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  return {
    lat: (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * Math.PI),
    lng: (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * Math.PI),
  }
}

/** WGS-84 → GCJ-02(入库/与小程序坐标对齐时使用) */
export function wgs84ToGcj02(
  lng: number,
  lat: number,
): { longitude: number; latitude: number } {
  if (isOutOfChina(lng, lat)) {
    return { longitude: lng, latitude: lat }
  }
  const d = delta(lng, lat)
  return { longitude: lng + d.lng, latitude: lat + d.lat }
}

/** GCJ-02 → WGS-84(在天地图等 WGS-84 底图上展示时使用) */
export function gcj02ToWgs84(
  lng: number,
  lat: number,
): { longitude: number; latitude: number } {
  if (isOutOfChina(lng, lat)) {
    return { longitude: lng, latitude: lat }
  }
  const d = delta(lng, lat)
  return { longitude: lng - d.lng, latitude: lat - d.lat }
}
