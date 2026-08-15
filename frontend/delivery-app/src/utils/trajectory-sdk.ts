/**
 * 轨迹上报定位 SDK 平台入口(native)
 *
 * react-native-fitness-geolocation 为纯原生模块(iOS/Android),模块顶层
 * `new NativeEventEmitter(Native)` 在 web 上不可用;由 trajectory-sdk.web.ts
 * 桩实现替换(Metro 按平台解析,web 打包不包含本 SDK)。
 *
 * 导出轨迹上报用到的成员:Geolocation(BackgroundGeolocation 风格生命周期 API)
 * 与 PermissionManager(权限申请)。
 */
import Geolocation, { PermissionManager } from 'react-native-fitness-geolocation';

export { Geolocation, PermissionManager };
export type {
  BackgroundGeolocationConfig,
  GeolocationResponse,
  FitnessPermissionResult,
} from 'react-native-fitness-geolocation';
