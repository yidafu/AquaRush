/**
 * storage 服务单测(AsyncStorage 封装)
 * 使用 @react-native-async-storage/async-storage 官方 jest mock。
 */
import AsyncStorage from '@react-native-async-storage/async-storage';

import { storage } from '@/services/storage';

// jest.mock 工厂内禁止引用外部变量,只能 require(官方文档推荐写法)
jest.mock('@react-native-async-storage/async-storage', () =>
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

describe('storage 服务', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  it('token 存取与清除', async () => {
    expect(await storage.getToken()).toBeNull();

    await storage.setToken('token-123');
    expect(await storage.getToken()).toBe('token-123');

    await storage.clearToken();
    expect(await storage.getToken()).toBeNull();
  });

  it('用户信息以 JSON 存取与清除', async () => {
    const info = { id: 1, username: 'zhangsan', realName: '张三', role: 'DELIVERER' };

    await storage.setUserInfo(info);
    expect(await storage.getUserInfo()).toEqual(info);

    await storage.clearUserInfo();
    expect(await storage.getUserInfo()).toBeNull();
  });

  it('用户信息 JSON 损坏时返回 null', async () => {
    await AsyncStorage.setItem('auth_user_info', '{broken json');
    expect(await storage.getUserInfo()).toBeNull();
  });

  it('clearAll 同时清空 token 与用户信息', async () => {
    await storage.setToken('token-123');
    await storage.setUserInfo({ id: 1, username: 'u', role: 'DELIVERER' });

    await storage.clearAll();

    expect(await storage.getToken()).toBeNull();
    expect(await storage.getUserInfo()).toBeNull();
  });
});
