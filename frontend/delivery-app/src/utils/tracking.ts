import AsyncStorage from '@react-native-async-storage/async-storage';
import { initTracking, type TrackingAdapter, type AuthContext } from '@aquarush/common/tracking';

import { API_BASE_URL } from '@/constants/api';

/**
 * 用户操作日志追踪器(delivery-app / RN 端装配)
 *
 * 核心逻辑见 @aquarush/common/tracking(SDK),本文件仅注入 RN 平台适配器:
 * - send:静默 fetch 上报(不走 networkManager,避免 401 触发会话过期登出副作用;
 *   上报失败绝不影响业务);
 * - 路由:模块级变量 currentRoute,由 usePageViewTracking 在路由变化时更新;
 * - 登录上下文:AsyncStorage 的 auth_user_info;token:auth_token(均异步读取)。
 */

const TOKEN_KEY = 'auth_token';
const USER_INFO_KEY = 'auth_user_info';

let currentRoute = '';

/** 路由变化时由 usePageViewTracking 调用,更新 tracker 使用的当前路由 */
export const setCurrentRoute = (path: string): void => {
  currentRoute = path;
};

const adapter: TrackingAdapter = {
  send: async (actions) => {
    const token = await AsyncStorage.getItem(TOKEN_KEY);
    const res = await fetch(`${API_BASE_URL}/api/logs/user-actions/batch`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ actions }),
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
  },
  getCurrentRoute: () => currentRoute,
  getAuthContext: async (): Promise<AuthContext> => {
    try {
      const raw = await AsyncStorage.getItem(USER_INFO_KEY);
      if (!raw) return {};
      const info = JSON.parse(raw);
      return {
        userId: info?.id != null ? String(info.id) : undefined,
        username: info?.username || info?.realName || undefined,
      };
    } catch {
      return {};
    }
  },
  hasToken: async () => Boolean(await AsyncStorage.getItem(TOKEN_KEY)),
};

/** 全局单例 */
export const userActionTracker = initTracking(adapter);
