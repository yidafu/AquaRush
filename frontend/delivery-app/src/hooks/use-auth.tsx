import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';

import { authService, AdminUserInfo } from '@/services/auth';
import { registerSessionExpiredHandler } from '@/utils/network-manager';
import { wsPushService } from '@/services/wsPush';

export const ROLE_DELIVERY_WORKER = 'DELIVERY_WORKER';
export const ROLE_ADMIN = 'ADMIN';
export const ROLE_SUPER_ADMIN = 'SUPER_ADMIN';

interface AuthContextValue {
  /** true until the stored session has been read from AsyncStorage. */
  isHydrating: boolean;
  isAuthenticated: boolean;
  userInfo: AdminUserInfo | null;
  /** ADMIN or SUPER_ADMIN — 有「管理」Tab 访问权。 */
  isAdmin: boolean;
  /** DELIVERY_WORKER — 送水员工作流。 */
  isDeliveryWorker: boolean;
  hasRole: (role: string) => boolean;
  login: (username: string, password: string) => Promise<AdminUserInfo>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isHydrating, setIsHydrating] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userInfo, setUserInfo] = useState<AdminUserInfo | null>(null);

  const role = userInfo?.role;

  useEffect(() => {
    registerSessionExpiredHandler(() => {
      setIsAuthenticated(false);
      setUserInfo(null);
    });

    (async () => {
      const session = await authService.getStoredSession();
      setIsAuthenticated(Boolean(session.token && session.userInfo));
      setUserInfo(session.userInfo);
      setIsHydrating(false);
    })();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const info = await authService.login(username, password);
    setIsAuthenticated(true);
    setUserInfo(info);
    // 登录成功后建立 WebSocket 实时推送连接(F-NTF-003,任务页也会兜底连接)
    if (info.id != null) {
      const session = await authService.getStoredSession();
      if (session.token) {
        wsPushService.connect(String(info.id), session.token).catch((e) => console.error('WS 连接失败:', e));
      }
    }
    return info;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setIsAuthenticated(false);
    setUserInfo(null);
    await wsPushService.disconnect();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isHydrating,
      isAuthenticated,
      userInfo,
      isAdmin: role === ROLE_ADMIN || role === ROLE_SUPER_ADMIN,
      isDeliveryWorker: role === ROLE_DELIVERY_WORKER,
      hasRole: (target) => role === target,
      login,
      logout,
    }),
    [isHydrating, isAuthenticated, userInfo, role, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
