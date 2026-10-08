/**
 * tracking.ts 的 RN 适配器单测
 *
 * tracking.ts 在模块加载时用 initTracking 创建全局单例(会启动定时 flush),
 * 测试不便直接驱动单例。这里 mock `@aquarush/common/tracking` 的 initTracking,
 * 捕获传入的 adapter(即本文件要验证的平台适配逻辑),返回假 tracker 避免定时器泄漏。
 */
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { TrackPayload, TrackingAdapter } from '@aquarush/common/tracking';

import { setCurrentRoute } from '@/utils/tracking';

// jest.mock 工厂内禁止引用外部变量,只能 require(官方文档推荐写法)
jest.mock('@react-native-async-storage/async-storage', () =>
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

/* eslint-disable no-var -- jest.mock 工厂执行早于文件体,var 提升避免 TDZ */
var capturedAdapter: TrackingAdapter | undefined;
var mockTrack = jest.fn();

jest.mock('@aquarush/common/tracking', () => {
  const actual = jest.requireActual('@aquarush/common/tracking');
  return {
    ...actual,
    initTracking: (adapter: TrackingAdapter) => {
      capturedAdapter = adapter;
      return {
        track: mockTrack,
        trackPageView: jest.fn(),
        flush: jest.fn(),
        dispose: jest.fn(),
        getQueueSize: () => 0,
      };
    },
  };
});
/* eslint-enable no-var */

const originalFetch = globalThis.fetch;

describe('tracking RN 适配器', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
    mockTrack.mockClear();
    globalThis.fetch = jest.fn();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('setCurrentRoute 更新适配器当前路由', () => {
    setCurrentRoute('/orders');
    expect(capturedAdapter?.getCurrentRoute()).toBe('/orders');

    setCurrentRoute('/profile');
    expect(capturedAdapter?.getCurrentRoute()).toBe('/profile');
  });

  it('send 携带 token 上报到后端批量接口', async () => {
    await AsyncStorage.setItem('auth_token', 'token-abc');
    (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, status: 200 });

    const actions: TrackPayload[] = [
      { actionType: 'CLICK', target: 'order-confirm', properties: {}, timestamp: 1 },
    ];
    await capturedAdapter?.send(actions);

    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:9090/api/logs/user-actions/batch',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer token-abc',
        }),
        body: JSON.stringify({ actions }),
      }),
    );
  });

  it('send 无 token 时不带 Authorization 头', async () => {
    (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, status: 200 });

    await capturedAdapter?.send([
      { actionType: 'CLICK', target: 'x', properties: {}, timestamp: 1 },
    ]);

    const [, init] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(init.headers).toEqual({ 'Content-Type': 'application/json' });
  });

  it('send 非 2xx 响应抛出错误(由 SDK 静默重入队)', async () => {
    (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: false, status: 500 });

    await expect(
      capturedAdapter?.send([{ actionType: 'CLICK', target: 'x', properties: {}, timestamp: 1 }]),
    ).rejects.toThrow('HTTP 500');
  });

  it('getAuthContext 从存储的用户信息解析 userId/username', async () => {
    await AsyncStorage.setItem(
      'auth_user_info',
      JSON.stringify({ id: 7, username: 'zhangsan', realName: '张三' }),
    );
    expect(await capturedAdapter?.getAuthContext()).toEqual({ userId: '7', username: 'zhangsan' });

    // username 缺失时回退 realName
    await AsyncStorage.setItem(
      'auth_user_info',
      JSON.stringify({ id: 8, realName: '张三' }),
    );
    expect(await capturedAdapter?.getAuthContext()).toEqual({ userId: '8', username: '张三' });

    // 无用户信息
    await AsyncStorage.removeItem('auth_user_info');
    expect(await capturedAdapter?.getAuthContext()).toEqual({});
  });

  it('getAuthContext 对损坏的 JSON 返回空对象', async () => {
    await AsyncStorage.setItem('auth_user_info', '{broken');
    expect(await capturedAdapter?.getAuthContext()).toEqual({});
  });

  it('hasToken 反映是否存在 token', async () => {
    expect(await capturedAdapter?.hasToken()).toBe(false);

    await AsyncStorage.setItem('auth_token', 'token-abc');
    expect(await capturedAdapter?.hasToken()).toBe(true);
  });
});
