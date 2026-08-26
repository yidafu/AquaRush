/**
 * 轨迹上报器单测(F-ADR-012,R-ADR-03)
 *
 * 通过 jest.mock 替换:
 * - react-native-fitness-geolocation 封装层(@/utils/trajectory-sdk,原生模块不可在 jest 运行);
 * - reportTrajectory(@/services/trajectory,GraphQL 网络调用);
 * - getOrderDetail(@/services/delivery,resumeActive 状态校验);
 * - AsyncStorage(官方 jest mock)。
 * @aquarush/common 保持真实导入(NetworkError/GraphQLError instanceof 判定依赖真实类)。
 */
import AsyncStorage from '@react-native-async-storage/async-storage';

import { GraphQLError, NetworkError } from '@aquarush/common';
import { getOrderDetail } from '@/services/delivery';
import { reportTrajectory } from '@/services/trajectory';
import { trajectoryReporter } from '@/utils/trajectory-reporter';
import { Geolocation, PermissionManager } from '@/utils/trajectory-sdk';

jest.mock('@react-native-async-storage/async-storage', () =>
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

jest.mock('@/services/trajectory', () => ({
  reportTrajectory: jest.fn(),
}));

jest.mock('@/services/delivery', () => ({
  getOrderDetail: jest.fn(),
}));

// 原生定位 SDK 封装层 mock:捕获 onLocation 回调,便于测试中模拟位置更新
jest.mock('@/utils/trajectory-sdk', () => {
  const locationListeners: ((position: unknown) => void | Promise<void>)[] = [];
  const subscription = { remove: jest.fn() };
  return {
    __test: { locationListeners, subscription },
    Geolocation: {
      ready: jest.fn().mockResolvedValue(null),
      start: jest.fn().mockResolvedValue(null),
      stop: jest.fn().mockResolvedValue(null),
      onLocation: jest.fn((cb: (position: unknown) => void | Promise<void>) => {
        locationListeners.push(cb);
        return subscription;
      }),
    },
    PermissionManager: {
      requestFitnessPermissions: jest.fn(),
    },
  };
});

/** 测试用位置对象最小形状(reporter 回调实际接收 SDK GeolocationResponse) */
interface MockPosition {
  coords: { longitude: number; latitude: number; accuracy: number };
  timestamp: number;
}

const sdkMock = jest.requireMock('@/utils/trajectory-sdk') as {
  __test: {
    locationListeners: ((position: MockPosition) => void | Promise<void>)[];
    subscription: { remove: jest.Mock };
  };
};
const locationListeners = sdkMock.__test.locationListeners;

const grantedPermission = {
  foregroundGranted: true,
  backgroundGranted: true,
  motionGranted: false,
  notificationsGranted: true,
  status: 'ready' as const,
};

const deniedPermission = {
  foregroundGranted: false,
  backgroundGranted: false,
  motionGranted: false,
  notificationsGranted: false,
  status: 'denied' as const,
  message: 'denied',
};

/** 位置回调参数(时间用本地 Date 构造,reportedAt 断言不受宿主时区影响) */
function makePosition(timestamp: number, longitude = 116.4, latitude = 39.9, accuracy = 5) {
  return { coords: { longitude, latitude, accuracy }, timestamp };
}

/**
 * 等待 reporter 异步链路执行完毕。
 * onLocation 回调为 fire-and-forget(void handleLocationUpdate),await 回调只等同步部分;
 * 上报/入队/补传等是后续 microtask 链,断言存储前需让 macrotask 边界冲刷。
 */
const flushAsync = () => new Promise<void>((resolve) => setTimeout(resolve, 0));

describe('trajectoryReporter 配送轨迹上报器', () => {
  beforeEach(async () => {
    jest.clearAllMocks();
    await AsyncStorage.clear();
    locationListeners.length = 0;
    await trajectoryReporter.stopAll();
    (PermissionManager.requestFitnessPermissions as jest.Mock).mockResolvedValue(grantedPermission);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('定位权限被拒:startForOrder 返回 false,不启动定位、不持久化活动订单', async () => {
    (PermissionManager.requestFitnessPermissions as jest.Mock).mockResolvedValueOnce(deniedPermission);

    const ok = await trajectoryReporter.startForOrder('1', 'NO1');

    expect(ok).toBe(false);
    expect(Geolocation.ready).not.toHaveBeenCalled();
    expect(Geolocation.start).not.toHaveBeenCalled();
    expect(Geolocation.onLocation).not.toHaveBeenCalled();
    expect(trajectoryReporter.isActive).toBe(false);
    expect(await AsyncStorage.getItem('trajectory_active_orders')).toBeNull();
  });

  it('权限授予:启动后台跟踪,活动订单含 orderNo 持久化', async () => {
    const ok = await trajectoryReporter.startForOrder('1', 'NO1');

    expect(ok).toBe(true);
    expect(PermissionManager.requestFitnessPermissions).toHaveBeenCalledWith({
      includeMotion: false,
      includeNotifications: true,
    });
    expect(Geolocation.ready).toHaveBeenCalledTimes(1);
    expect(Geolocation.start).toHaveBeenCalledTimes(1);
    expect(Geolocation.onLocation).toHaveBeenCalledTimes(1);
    expect(trajectoryReporter.isActive).toBe(true);

    const raw = await AsyncStorage.getItem('trajectory_active_orders');
    expect(raw).not.toBeNull();
    expect(JSON.parse(raw!)).toEqual([{ orderId: '1', orderNo: 'NO1' }]);
  });

  it('位置回调:每 5s 节流调 reportTrajectory(坐标/精度/ISO_LOCAL_DATE_TIME 时间)', async () => {
    await trajectoryReporter.startForOrder('1', 'NO1');

    const nowSpy = jest.spyOn(Date, 'now');
    const t0 = new Date(2026, 7, 9, 12, 0, 0).getTime();
    nowSpy.mockReturnValue(t0);

    await locationListeners[0](makePosition(t0));
    await flushAsync();
    expect(reportTrajectory).toHaveBeenCalledTimes(1);
    expect(reportTrajectory).toHaveBeenCalledWith('1', 116.4, 39.9, 5, '2026-08-09T12:00:00');

    // 5s 内再次更新:节流跳过
    nowSpy.mockReturnValue(t0 + 1000);
    await locationListeners[0](makePosition(t0 + 1000));
    await flushAsync();
    expect(reportTrajectory).toHaveBeenCalledTimes(1);

    // 超过 5s:再次上报
    nowSpy.mockReturnValue(t0 + 6000);
    await locationListeners[0](makePosition(t0 + 6000));
    await flushAsync();
    expect(reportTrajectory).toHaveBeenCalledTimes(2);
  });

  it('弱网失败(NetworkError):缓存补传队列,恢复后 FIFO 补传', async () => {
    await trajectoryReporter.startForOrder('1', 'NO1');

    const nowSpy = jest.spyOn(Date, 'now');
    const t0 = new Date(2026, 7, 9, 12, 0, 0).getTime();
    nowSpy.mockReturnValue(t0);

    // 第 1 次上报:网络失败 → 入队
    (reportTrajectory as jest.Mock).mockRejectedValueOnce(new NetworkError('request timeout'));
    await locationListeners[0](makePosition(t0));
    await flushAsync();
    expect(reportTrajectory).toHaveBeenCalledTimes(1);

    const pendingRaw = await AsyncStorage.getItem('trajectory_pending_points');
    expect(pendingRaw).not.toBeNull();
    expect(JSON.parse(pendingRaw!)).toEqual([
      { orderId: '1', longitude: 116.4, latitude: 39.9, accuracy: 5, reportedAt: '2026-08-09T12:00:00' },
    ]);

    // 5s 后第 2 次上报成功 → 自动补传队列中的失败点
    nowSpy.mockReturnValue(t0 + 6000);
    await locationListeners[0](makePosition(t0 + 6000));
    await flushAsync();
    expect(reportTrajectory).toHaveBeenCalledTimes(3); // 第 2 次上报 + 补传 1 点
    expect(await AsyncStorage.getItem('trajectory_pending_points')).toBeNull();
  });

  it('服务端拒绝(GraphQLError):停止该订单上报、不入补传队列', async () => {
    await trajectoryReporter.startForOrder('1', 'NO1');

    const nowSpy = jest.spyOn(Date, 'now');
    const t0 = new Date(2026, 7, 9, 12, 0, 0).getTime();
    nowSpy.mockReturnValue(t0);

    (reportTrajectory as jest.Mock).mockRejectedValueOnce(
      new GraphQLError('订单不在配送中,无法上报轨迹'),
    );
    await locationListeners[0](makePosition(t0));
    await flushAsync();

    expect(reportTrajectory).toHaveBeenCalledTimes(1);
    expect(await AsyncStorage.getItem('trajectory_pending_points')).toBeNull();
    expect(await AsyncStorage.getItem('trajectory_active_orders')).toBeNull();
    expect(trajectoryReporter.isActive).toBe(false);
    // 最后一个活动订单停止 → 停止定位并取消订阅
    expect(Geolocation.stop).toHaveBeenCalled();
    expect(sdkMock.__test.subscription.remove).toHaveBeenCalled();
  });

  it('stopForOrder:停止定位并取消订阅,清空该订单补传点', async () => {
    await trajectoryReporter.startForOrder('1', 'NO1');
    await AsyncStorage.setItem(
      'trajectory_pending_points',
      JSON.stringify([{ orderId: '1', longitude: 1, latitude: 2, accuracy: null, reportedAt: '2026-08-09T12:00:00' }]),
    );

    await trajectoryReporter.stopForOrder('1');

    expect(Geolocation.stop).toHaveBeenCalled();
    expect(sdkMock.__test.subscription.remove).toHaveBeenCalled();
    expect(trajectoryReporter.isActive).toBe(false);
    expect(await AsyncStorage.getItem('trajectory_pending_points')).toBeNull();
  });

  it('resumeActive:校验订单状态,仅恢复 DELIVERING 订单', async () => {
    await AsyncStorage.setItem(
      'trajectory_active_orders',
      JSON.stringify([
        { orderId: '1', orderNo: 'NO1' },
        { orderId: '2', orderNo: 'NO2' },
      ]),
    );
    (getOrderDetail as jest.Mock)
      .mockResolvedValueOnce({ orderByNo: { status: 'DELIVERING' } })
      .mockResolvedValueOnce({ orderByNo: { status: 'COMPLETED' } });

    await trajectoryReporter.resumeActive();

    expect(getOrderDetail).toHaveBeenCalledTimes(2);
    // 仅 DELIVERING 的订单恢复上报
    expect(trajectoryReporter.isActive).toBe(true);
    expect(Geolocation.onLocation).toHaveBeenCalledTimes(1);
    const raw = await AsyncStorage.getItem('trajectory_active_orders');
    expect(JSON.parse(raw!)).toEqual([{ orderId: '1', orderNo: 'NO1' }]);
  });

  it('resumeActive:全部订单已非配送中时清理为空;损坏数据清空兜底不抛错', async () => {
    await AsyncStorage.setItem(
      'trajectory_active_orders',
      JSON.stringify([{ orderId: '1', orderNo: 'NO1' }]),
    );
    (getOrderDetail as jest.Mock).mockResolvedValue({ orderByNo: { status: 'CANCELLED' } });

    await trajectoryReporter.resumeActive();
    expect(trajectoryReporter.isActive).toBe(false);
    expect(await AsyncStorage.getItem('trajectory_active_orders')).toBeNull();

    // 损坏 JSON
    await AsyncStorage.setItem('trajectory_active_orders', '{broken json');
    await expect(trajectoryReporter.resumeActive()).resolves.toBeUndefined();
    expect(await AsyncStorage.getItem('trajectory_active_orders')).toBeNull();
  });

  it('stopAll:清空活动订单与补传队列并停止定位', async () => {
    await trajectoryReporter.startForOrder('1', 'NO1');
    await AsyncStorage.setItem(
      'trajectory_pending_points',
      JSON.stringify([{ orderId: '1', longitude: 1, latitude: 2, accuracy: null, reportedAt: '2026-08-09T12:00:00' }]),
    );

    await trajectoryReporter.stopAll();

    expect(Geolocation.stop).toHaveBeenCalled();
    expect(trajectoryReporter.isActive).toBe(false);
    expect(await AsyncStorage.getItem('trajectory_active_orders')).toBeNull();
    expect(await AsyncStorage.getItem('trajectory_pending_points')).toBeNull();
  });
});
