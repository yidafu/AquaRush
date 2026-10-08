/**
 * 地理编码服务(管理后台)
 *
 * 决策(2026 地图迁移):底图使用 MapLibre + 天地图,坐标存储/接口统一 **WGS-84**;
 * 正向/逆地理编码继续使用腾讯 WebService API(解析质量更好)。
 * 注意:腾讯 API 返回/入参为 **GCJ-02** —— 入库前须 `gcj02ToWgs84`,
 * 发起逆地理编码请求前须 `wgs84ToGcj02`(见 AddressMapPicker)。
 */
export interface GeocodeResult {
  /** GCJ-02 经度(腾讯返回;入库前需转 WGS-84) */
  longitude: number;
  /** GCJ-02 纬度 */
  latitude: number;
  address: string;
}

/** 腾讯地图 WebService Key(与用户端 constants/qqmap.ts 一致) */
export const TENCENT_MAP_KEY = '3YEBZ-G233L-GN7P7-M7EN6-YNLOH-VVBTT';

/** 正向地理编码:地址 → 坐标(返回 GCJ-02) */
export const geocodeAddress = async (
  address: string,
  region?: string,
): Promise<GeocodeResult | null> => {
  if (!address) return null;

  try {
    const encodedAddress = encodeURIComponent(address);
    const url = `https://apis.map.qq.com/ws/geocoder/v1/?address=${encodedAddress}&key=${TENCENT_MAP_KEY}${region ? `&region=${encodeURIComponent(region)}` : ''}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status === 0 && data.result) {
      const location = data.result.location;
      return {
        longitude: location.lng,
        latitude: location.lat,
        address: data.result.title || address,
      };
    } else {
      console.error('Geocoding failed:', data.message);
      return null;
    }
  } catch (error) {
    console.error('Geocoding error:', error);
    return null;
  }
};

/** 逆地理编码:坐标 → 地址(入参为 GCJ-02) */
export const reverseGeocodeAddress = async (
  lng: number,
  lat: number,
): Promise<string> => {
  try {
    const url = `https://apis.map.qq.com/ws/geocoder/v1/?location=${lat},${lng}&key=${TENCENT_MAP_KEY}&get_poi_info=0`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status === 0 && data.result) {
      return data.result.address;
    }
    return '';
  } catch (error) {
    console.error('Reverse geocoding error:', error);
    return '';
  }
};
