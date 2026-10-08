/**
 * wsPush 服务单测:WS 推送 → 系统通知栏(替代 toast)的内容映射与呈现。
 * 仅测纯逻辑:buildPushNotificationContent 门控/字段、showPushNotification 的调度调用与 web 跳过。
 */
import { Platform } from 'react-native';
import * as Notifications from 'expo-notifications';

import { buildPushNotificationContent, showPushNotification } from '@/services/wsPush';

// wsPush → auth → storage 链路需要 AsyncStorage 官方 jest mock(与 storage.test.ts 一致)
jest.mock('@react-native-async-storage/async-storage', () =>
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

// jest.mock 工厂内不允许引用外部变量,直接内联 jest.fn()
jest.mock('expo-notifications', () => ({
  setNotificationHandler: jest.fn(),
  scheduleNotificationAsync: jest.fn().mockResolvedValue('notification-id'),
  setNotificationChannelAsync: jest.fn().mockResolvedValue({}),
  getPermissionsAsync: jest.fn().mockResolvedValue({ status: 'granted' }),
  requestPermissionsAsync: jest.fn().mockResolvedValue({ status: 'granted' }),
  addNotificationResponseReceivedListener: jest.fn(() => ({ remove: jest.fn() })),
  AndroidImportance: { HIGH: 4 },
}));

jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));

const scheduleNotificationAsync = Notifications.scheduleNotificationAsync as jest.Mock;

describe('buildPushNotificationContent', () => {
  it('NEW_ORDER 场景 → 「新订单待接单」+ 摘要,携带跳转数据', () => {
    const content = buildPushNotificationContent({
      idempotencyKey: 'k-1',
      scene: 'NEW_ORDER',
      summary: '订单 NO.20260825001 待接单',
      relatedNo: 'NO.20260825001',
    });
    expect(content).toEqual({
      title: '新订单待接单',
      body: '订单 NO.20260825001 待接单',
      data: {
        orderNo: 'NO.20260825001',
        scene: 'NEW_ORDER',
        type: null,
        idempotencyKey: 'k-1',
      },
    });
  });

  it('NEW_ORDER 场景无摘要 → 默认文案', () => {
    expect(buildPushNotificationContent({ idempotencyKey: 'k-2', scene: 'NEW_ORDER' })?.body).toBe(
      '有新的待接单订单,请及时处理',
    );
  });

  it('ORDER 类型 → 标题取 message.title,摘要取 summary', () => {
    const content = buildPushNotificationContent({
      idempotencyKey: 'k-3',
      type: 'ORDER',
      title: '新派单',
      summary: '您有一条新派单',
    });
    expect(content).toEqual({
      title: '新派单',
      body: '您有一条新派单',
      data: {
        orderNo: null,
        scene: null,
        type: 'ORDER',
        idempotencyKey: 'k-3',
      },
    });
  });

  it('ORDER 类型无标题/摘要 → 缺省标题,无 body', () => {
    const content = buildPushNotificationContent({ idempotencyKey: 'k-4', type: 'ORDER' });
    expect(content?.title).toBe('新消息');
    expect(content?.body).toBeUndefined();
  });

  it('SYSTEM / ANNOUNCEMENT / FEEDBACK 不弹系统通知', () => {
    expect(buildPushNotificationContent({ idempotencyKey: 'k-5', type: 'SYSTEM' })).toBeNull();
    expect(buildPushNotificationContent({ idempotencyKey: 'k-6', type: 'ANNOUNCEMENT' })).toBeNull();
    expect(buildPushNotificationContent({ idempotencyKey: 'k-7', type: 'FEEDBACK' })).toBeNull();
  });
});

describe('showPushNotification', () => {
  beforeEach(() => {
    scheduleNotificationAsync.mockClear();
  });

  it('原生端:调度本地通知(trigger: null 即时呈现于系统通知栏)', async () => {
    await showPushNotification({
      idempotencyKey: 'k-1',
      scene: 'NEW_ORDER',
      summary: '有新的待接单订单',
    });
    expect(scheduleNotificationAsync).toHaveBeenCalledTimes(1);
    expect(scheduleNotificationAsync).toHaveBeenCalledWith({
      content: {
        title: '新订单待接单',
        body: '有新的待接单订单',
        data: {
          orderNo: null,
          scene: 'NEW_ORDER',
          type: null,
          idempotencyKey: 'k-1',
        },
      },
      trigger: null,
    });
  });

  it('非通知门控消息(如 SYSTEM)不调度', async () => {
    await showPushNotification({ idempotencyKey: 'k-2', type: 'SYSTEM' });
    expect(scheduleNotificationAsync).not.toHaveBeenCalled();
  });

  it('web 无系统通知栏:静默跳过,不调度', async () => {
    const originalOs = Platform.OS;
    Platform.OS = 'web';
    try {
      await showPushNotification({ idempotencyKey: 'k-3', scene: 'NEW_ORDER' });
      expect(scheduleNotificationAsync).not.toHaveBeenCalled();
    } finally {
      Platform.OS = originalOs;
    }
  });
});
