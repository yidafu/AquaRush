import { networkManager } from '@/utils/network-manager';
import { API_BASE_URL } from '@/constants/api';
import { storage, StoredUserInfo } from '@/services/storage';

export type AdminUserInfo = StoredUserInfo;

export interface LoginResponseData {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  userInfo: AdminUserInfo;
}

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string | null;
  code?: string | null;
}

/**
 * Username/password login against the existing aqua-admin auth endpoint.
 * Delivery workers use the `delivery_{workerId}` account (default password 123456).
 */
export const authService = {
  async login(username: string, password: string): Promise<AdminUserInfo> {
    const res = await networkManager.restRequest<ApiResponse<LoginResponseData>>({
      url: `${API_BASE_URL}/api/auth/login`,
      method: 'POST',
      data: { username, password },
      skipAuth: true, // no token yet
    });

    if (!res.success || !res.data) {
      throw new Error(res.message || '登录失败');
    }

    await storage.setToken(res.data.accessToken);
    await storage.setUserInfo(res.data.userInfo);
    return res.data.userInfo;
  },

  async logout(): Promise<void> {
    await storage.clearAll();
  },

  async getStoredSession(): Promise<{ token: string | null; userInfo: AdminUserInfo | null }> {
    const [token, userInfo] = await Promise.all([storage.getToken(), storage.getUserInfo()]);
    return { token, userInfo };
  },
};
