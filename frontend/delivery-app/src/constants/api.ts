import Constants from 'expo-constants';
import { Platform } from 'react-native';

/**
 * Resolve a host that can reach the backend from the current runtime.
 * - Web/iOS simulator: `localhost` works.
 * - Android emulator: the host machine is `10.0.2.2`.
 * - Physical device / Expo Go: use the dev-server LAN IP so the device can reach the host.
 */
function defaultHost(): string {
  const hostUri = Constants.expoConfig?.hostUri;
  const host = hostUri?.split(':')[0];
  if (host && host !== 'localhost' && host !== '127.0.0.1') {
    return `http://${host}:9090`;
  }
  return Platform.OS === 'android' ? 'http://10.0.2.2:9090' : 'http://localhost:9090';
}

/** Override via `EXPO_PUBLIC_API_BASE_URL` (e.g. a real server on the LAN). */
export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? defaultHost();

/** aqua-admin GraphQL endpoint (port 9090). */
export const GRAPHQL_URL = `${API_BASE_URL}/graphql`;

/** Delivery-photo upload endpoint (multipart `file`). */
export const UPLOAD_URL = `${API_BASE_URL}/api/v1/storage/files`;

/** WebSocket 实时推送端点(与 API 同一主机,http→ws)。 */
export const WS_PUSH_URL = `${API_BASE_URL.replace(/^http/, 'ws')}/ws/notifications`;

/**
 * Resolve the admin-client host that serves the user-weapp H5 build at `/weapp/`.
 * 开发:admin dev server(5173,代理 9090 且托管 public/weapp);
 * 生产:反代(Caddy/Nginx)托管的 admin 静态产物,用 EXPO_PUBLIC_H5_BASE_URL 覆盖(如 https://example.com)。
 */
function defaultH5Host(): string {
  const hostUri = Constants.expoConfig?.hostUri;
  const host = hostUri?.split(':')[0];
  if (host && host !== 'localhost' && host !== '127.0.0.1') {
    return `http://${host}:5173`;
  }
  return Platform.OS === 'android' ? 'http://10.0.2.2:5173' : 'http://localhost:5173';
}

/** Override via `EXPO_PUBLIC_H5_BASE_URL` (e.g. a real server on the LAN). */
export const H5_BASE_URL = process.env.EXPO_PUBLIC_H5_BASE_URL ?? defaultH5Host();

/**
 * 商品详情 H5 页 URL(与 admin 预览同构:user-weapp 商详页编译产物 + hash 路由)。
 * `from=rn` 让 H5 端识别为 RN WebView 场景,隐藏购买/收藏等交互(纯展示)。
 */
export const productDetailH5Url = (id: string) =>
  `${H5_BASE_URL}/weapp/index.html#/pages/product-detail/index?id=${encodeURIComponent(id)}&preview=true&from=rn`;
