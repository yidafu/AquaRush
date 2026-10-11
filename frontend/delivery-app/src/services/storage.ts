import AsyncStorage from '@react-native-async-storage/async-storage';

/** Session token key (kept as `auth_token` for backward compatibility with existing logins). */
const TOKEN_KEY = 'auth_token';
const USER_INFO_KEY = 'auth_user_info';
/** 记住账号(仅用户名,不缓存密码)——登录页「记住账号」开关。 */
const REMEMBERED_USERNAME_KEY = 'auth_remembered_username';

export interface StoredUserInfo {
  id: number;
  username: string;
  realName?: string | null;
  role: string;
}

export const storage = {
  async setToken(token: string): Promise<void> {
    await AsyncStorage.setItem(TOKEN_KEY, token);
  },

  async getToken(): Promise<string | null> {
    return AsyncStorage.getItem(TOKEN_KEY);
  },

  async clearToken(): Promise<void> {
    await AsyncStorage.removeItem(TOKEN_KEY);
  },

  async setUserInfo(info: StoredUserInfo): Promise<void> {
    await AsyncStorage.setItem(USER_INFO_KEY, JSON.stringify(info));
  },

  async getUserInfo(): Promise<StoredUserInfo | null> {
    const raw = await AsyncStorage.getItem(USER_INFO_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredUserInfo;
    } catch {
      return null;
    }
  },

  async clearUserInfo(): Promise<void> {
    await AsyncStorage.removeItem(USER_INFO_KEY);
  },

  async clearAll(): Promise<void> {
    await Promise.all([AsyncStorage.removeItem(TOKEN_KEY), AsyncStorage.removeItem(USER_INFO_KEY)]);
  },

  /** 记住账号:仅存用户名(原型 login.html——「记住账号」仅本地记住用户名,不缓存密码) */
  async setRememberedUsername(username: string): Promise<void> {
    await AsyncStorage.setItem(REMEMBERED_USERNAME_KEY, username);
  },

  async getRememberedUsername(): Promise<string | null> {
    return AsyncStorage.getItem(REMEMBERED_USERNAME_KEY);
  },

  async clearRememberedUsername(): Promise<void> {
    await AsyncStorage.removeItem(REMEMBERED_USERNAME_KEY);
  },
};
