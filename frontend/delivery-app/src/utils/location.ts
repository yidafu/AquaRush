import { Platform } from 'react-native';

export interface GeoPosition {
  longitude: number;
  latitude: number;
}

/**
 * 获取当前定位(原生端 expo-location,Expo Go 内置;web 端浏览器 Geolocation)。
 *
 * 坐标体系:GPS/浏览器返回 WGS-84,与后端地址坐标存储一致(PRD/05-address-delivery.md
 * §坐标体系),可直接用于修正订单地址定位(F-ORD-024)。
 *
 * 失败(权限拒绝/定位服务不可用/超时)返回 null,调用方提示用户。
 */
export async function getCurrentPosition(timeoutMs = 10000): Promise<GeoPosition | null> {
  try {
    if (Platform.OS === 'web') {
      return await getBrowserPosition(timeoutMs);
    }
    const Location = await import('expo-location');
    const { status } = await Location.requestForegroundPermissionsAsync();
    if (status !== 'granted') {
      return null;
    }
    const position = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.Balanced,
    });
    return {
      longitude: position.coords.longitude,
      latitude: position.coords.latitude,
    };
  } catch (e) {
    console.warn('获取当前位置失败:', e);
    return null;
  }
}

function getBrowserPosition(timeoutMs: number): Promise<GeoPosition | null> {
  return new Promise((resolve) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      resolve(null);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ longitude: pos.coords.longitude, latitude: pos.coords.latitude }),
      () => resolve(null),
      { timeout: timeoutMs, maximumAge: 30000, enableHighAccuracy: true },
    );
  });
}
