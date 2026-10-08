import AsyncStorage from '@react-native-async-storage/async-storage';

/** Session token key (kept as `auth_token` for backward compatibility with existing logins). */
const TOKEN_KEY = 'auth_token';
const USER_INFO_KEY = 'auth_user_info';

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
};
