/**
 * 轨迹上报定位 SDK Web 端桩实现。
 *
 * react-native-fitness-geolocation 为纯原生模块,web 无实现(模块顶层
 * `NativeEventEmitter(Native)` 不可用,无法静态导入)。此文件由 Metro 在 web
 * 平台替代 trajectory-sdk.ts(平台文件解析优先),所有能力返回「不可用」,
 * 轨迹上报在 web 端静默降级(F-ADR-012 仅原生 App 使用)。
 */
import type {
  BackgroundGeolocationConfig,
  FitnessPermissionResult,
  GeolocationResponse,
} from 'react-native-fitness-geolocation';

const deniedPermission: FitnessPermissionResult = {
  foregroundGranted: false,
  backgroundGranted: false,
  motionGranted: false,
  notificationsGranted: false,
  status: 'denied',
  message: 'location not supported on web',
};

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
    error?.({ code: 2, message: 'location not supported on web' });
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
    return deniedPermission;
  },
  async openSettings(): Promise<void> {},
  async openBatterySettings(): Promise<void> {},
};
