/**
 * 轨迹上报定位 SDK 占位实现(native)
 *
 * 已移除 react-native-fitness-geolocation(原生 GPS + SQLite 持久化定位 SDK,
 * 其 Android 源码存在编译问题且不再需要),轨迹上报功能暂停。
 * 本文件保留原导出形状(Geolocation / PermissionManager 及类型),所有能力
 * 均为空实现:调用安全、不抛错、不产生任何定位行为,便于后续接入替代 SDK
 * 时仅替换本文件。全平台(含 web)统一使用本占位实现。
 */

export interface BackgroundGeolocationConfig {
  authorizationLevel?: string;
  enableHighAccuracy?: boolean;
  desiredAccuracy?: number;
  distanceFilter?: number;
  locationUpdateInterval?: number;
  fastestLocationUpdateInterval?: number;
  trackingMode?: string;
  pausesLocationUpdatesAutomatically?: boolean;
  showsBackgroundLocationIndicator?: boolean;
  stopOnTerminate?: boolean;
  notificationTitle?: string;
  notificationText?: string;
}

export interface GeolocationResponse {
  coords: {
    longitude: number;
    latitude: number;
    accuracy: number | null;
  };
  timestamp?: number;
}

export interface FitnessPermissionResult {
  foregroundGranted: boolean;
  backgroundGranted: boolean;
  motionGranted: boolean;
  notificationsGranted: boolean;
  status: 'granted' | 'denied';
  message?: string;
}

type SuccessCallback = (position: GeolocationResponse) => void | Promise<void>;
type ErrorCallback = (error: { code: number; message: string }) => void;

function noopSubscription() {
  return { remove(): void {} };
}

export const Geolocation = {
  async ready(_config?: BackgroundGeolocationConfig): Promise<unknown> {
    return null;
  },
  async start(): Promise<unknown> {
    return null;
  },
  async stop(): Promise<unknown> {
    return null;
  },
  onLocation(_success: SuccessCallback, _error?: ErrorCallback) {
    return noopSubscription();
  },
  getCurrentPosition(success: SuccessCallback, error?: ErrorCallback): void {
    error?.({ code: 2, message: 'location reporting is disabled' });
  },
  watchPosition(): number {
    return -1;
  },
  clearWatch(_watchId: number): void {},
  async getAuthorizationStatus(): Promise<{ status: string; always: boolean }> {
    return { status: 'denied', always: false };
  },
};

export const PermissionManager = {
  async getStatus(): Promise<{ location: string; always: boolean }> {
    return { location: 'denied', always: false };
  },
  async requestForeground(): Promise<boolean> {
    return false;
  },
  async requestBackground(): Promise<boolean> {
    return false;
  },
  async requestMotion(): Promise<boolean> {
    return false;
  },
  async requestNotifications(): Promise<boolean> {
    return false;
  },
  async requestFitnessPermissions(_options?: {
    includeMotion?: boolean;
    includeNotifications?: boolean;
  }): Promise<FitnessPermissionResult> {
    return {
      foregroundGranted: false,
      backgroundGranted: false,
      motionGranted: false,
      notificationsGranted: false,
      status: 'denied',
      message: 'location reporting is disabled',
    };
  },
  async openSettings(): Promise<void> {},
  async openBatterySettings(): Promise<void> {},
};
