import AsyncStorage from '@react-native-async-storage/async-storage';

import { reportTrajectory } from '@/services/trajectory';
import { Geolocation, PermissionManager } from '@/utils/trajectory-sdk';
import type { GeolocationResponse } from '@/utils/trajectory-sdk';

/**
 * 送水工端配送轨迹上报器(F-ADR-012,R-ADR-03)
 *
 * 定位 SDK:react-native-fitness-geolocation(原生 GPS + SQLite 持久化,无数据丢失;
 * Web 端由 trajectory-sdk.web.ts 桩实现替换,自动降级不启动)。
 * - 订单进入「配送中」(开始配送)后自动启动持续定位,每 5s 上报一次位置点;
 * - 「完成配送/订单取消」停止上报;App 重启/重新登录后自动恢复配送中订单的轨迹上报;
 * - 弱网上报失败:轨迹点缓存到 AsyncStorage,恢复后补传(不阻塞配送主流程);
 * - 上报频率固定 5s(客户端实现,后台可配置为开放问题,见 PRD G65)。
 */
export const REPORT_INTERVAL_MS = 5000;

interface PendingPoint {
  orderId: string;
  longitude: number;
  latitude: number;
  accuracy: number | null;
  reportedAt: string;
}

const ACTIVE_ORDERS_KEY = 'trajectory_active_orders';
const PENDING_POINTS_KEY = 'trajectory_pending_points';
const MAX_PENDING_POINTS = 200;

function toIso(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

class TrajectoryReporter {
  private locationSubscription: { remove(): void } | null = null;
  private activeOrders = new Set<string>();
  private lastReportAt: Record<string, number> = {};
  private startPromise: Promise<boolean> | null = null;

  /** 当前是否有配送中订单在持续上报 */
  get isActive(): boolean {
    return this.activeOrders.size > 0;
  }

  /**
   * 开始为某订单上报轨迹(订单进入「配送中」时调用;可重复调用,幂等)
   */
  async startForOrder(orderId: string): Promise<boolean> {
    if (this.activeOrders.has(orderId)) {
      return this.locationSubscription != null;
    }
    this.activeOrders.add(orderId);
    await this.persistActiveOrders();

    // 多个订单并发启动时复用同一个启动流程(幂等)
    if (!this.startPromise) {
      this.startPromise = this.ensureTracking();
    }
    const ok = await this.startPromise;
    this.startPromise = null;
    if (!ok) {
      // 定位权限被拒/启动失败:移除活动订单,避免误报已启动
      this.activeOrders.delete(orderId);
      await this.persistActiveOrders();
    }
    return ok;
  }

  /**
   * 停止为某订单上报轨迹(「完成配送/订单取消」时调用)
   */
  async stopForOrder(orderId: string): Promise<void> {
    if (!this.activeOrders.has(orderId)) return;
    this.activeOrders.delete(orderId);
    delete this.lastReportAt[orderId];
    await this.persistActiveOrders();
    if (this.activeOrders.size === 0) {
      await this.stopTracking();
    }
  }

  /**
   * 恢复持久化的配送中订单轨迹上报(App 启动/登录成功后调用)
   */
  async resumeActive(): Promise<void> {
    const raw = await AsyncStorage.getItem(ACTIVE_ORDERS_KEY);
    if (!raw) return;
    try {
      const ids = JSON.parse(raw) as string[];
      if (ids.length === 0) return;
      ids.forEach((id) => this.activeOrders.add(id));
      await this.ensureTracking();
    } catch {
      // 数据损坏时清空,避免阻塞
      await AsyncStorage.removeItem(ACTIVE_ORDERS_KEY);
    }
  }

  /** 停止全部上报(登出时调用) */
  async stopAll(): Promise<void> {
    this.activeOrders.clear();
    this.lastReportAt = {};
    await this.persistActiveOrders();
    await this.stopTracking();
  }

  // ==================== 内部实现 ====================

  private async persistActiveOrders(): Promise<void> {
    await AsyncStorage.setItem(ACTIVE_ORDERS_KEY, JSON.stringify([...this.activeOrders]));
  }

  private async ensureTracking(): Promise<boolean> {
    // 1. 定位权限(前台 + 后台;运动识别与配送无关,不申请)
    try {
      const perm = await PermissionManager.requestFitnessPermissions({
        includeMotion: false,
        // Android 13+ 前台服务通知需要 POST_NOTIFICATIONS,否则系统可能不显示常驻通知
        includeNotifications: true,
      });
      if (perm.status === 'denied' || (!perm.foregroundGranted && !perm.backgroundGranted)) {
        // R-ADR-03:定位权限被拒 → 上报停止,配送主流程不受影响(PRD 13-misc.md §履约类)
        return false;
      }
    } catch {
      return false;
    }

    // 2. 全局配置(ready 幂等;重复调用仅重配置)
    try {
      await Geolocation.ready({
        authorizationLevel: 'whenInUse',
        enableHighAccuracy: true,
        desiredAccuracy: 10,
        distanceFilter: 0,
        // R-ADR-03 上报频率 5s
        locationUpdateInterval: REPORT_INTERVAL_MS,
        fastestLocationUpdateInterval: 2000,
        trackingMode: 'navigation',
        // 不启用运动自动暂停(送水场景电梯/走动频繁,保持持续定位)
        pausesLocationUpdatesAutomatically: false,
        showsBackgroundLocationIndicator: true,
        stopOnTerminate: false,
        notificationTitle: '配送轨迹上报中',
        notificationText: '正在持续上报配送位置',
      });
    } catch {
      return false;
    }

    // 3. 订阅位置更新(原生 SQLite 先落库再回调 JS,弱网/桥挂不丢点)
    this.locationSubscription = Geolocation.onLocation((location) => {
      void this.handleLocationUpdate(location);
    });

    // 4. 启动原生跟踪(Android 前台服务 + iOS 后台 GPS)
    try {
      await Geolocation.start();
    } catch {
      this.locationSubscription?.remove();
      this.locationSubscription = null;
      return false;
    }
    return true;
  }

  private async stopTracking(): Promise<void> {
    this.locationSubscription?.remove();
    this.locationSubscription = null;
    try {
      await Geolocation.stop();
    } catch {
      // 停止失败不阻塞主流程(下次启动会重新配置)
    }
  }

  private async handleLocationUpdate(location: GeolocationResponse): Promise<void> {
    const { longitude, latitude, accuracy } = location.coords;
    const now = Date.now();
    const at = new Date(location.timestamp ?? now);
    // 对每个配送中订单按 5s 节流上报(同一位置更新为多个订单复用)
    for (const orderId of [...this.activeOrders]) {
      if (now - (this.lastReportAt[orderId] ?? 0) >= REPORT_INTERVAL_MS) {
        this.lastReportAt[orderId] = now;
        await this.report(orderId, longitude, latitude, accuracy ?? null, at);
      }
    }
  }

  private async report(
    orderId: string,
    longitude: number,
    latitude: number,
    accuracy: number | null,
    at: Date,
  ): Promise<void> {
    try {
      await reportTrajectory(orderId, longitude, latitude, accuracy, toIso(at));
      // 成功:补传缓存的失败点
      await this.flushPending();
    } catch {
      // 弱网失败:缓存轨迹点,恢复后补传(不阻塞配送主流程)
      await this.enqueuePending({ orderId, longitude, latitude, accuracy, reportedAt: toIso(at) });
    }
  }

  private async enqueuePending(point: PendingPoint): Promise<void> {
    const raw = await AsyncStorage.getItem(PENDING_POINTS_KEY);
    let points: PendingPoint[] = [];
    try {
      points = raw ? (JSON.parse(raw) as PendingPoint[]) : [];
    } catch {
      points = [];
    }
    points.push(point);
    // 限制缓存上限,防止无限增长
    if (points.length > MAX_PENDING_POINTS) {
      points = points.slice(points.length - MAX_PENDING_POINTS);
    }
    await AsyncStorage.setItem(PENDING_POINTS_KEY, JSON.stringify(points));
  }

  private async flushPending(): Promise<void> {
    const raw = await AsyncStorage.getItem(PENDING_POINTS_KEY);
    if (!raw) return;
    let points: PendingPoint[];
    try {
      points = JSON.parse(raw) as PendingPoint[];
    } catch {
      await AsyncStorage.removeItem(PENDING_POINTS_KEY);
      return;
    }
    if (points.length === 0) return;
    // 逐条补传;任一失败则保留剩余(下次成功再补)
    const remaining: PendingPoint[] = [];
    for (const p of points) {
      try {
        await reportTrajectory(p.orderId, p.longitude, p.latitude, p.accuracy, p.reportedAt);
      } catch {
        remaining.push(p);
      }
    }
    if (remaining.length === 0) {
      await AsyncStorage.removeItem(PENDING_POINTS_KEY);
    } else {
      await AsyncStorage.setItem(PENDING_POINTS_KEY, JSON.stringify(remaining));
    }
  }
}

/** 单例:全 App 共享一个轨迹上报器(由订单详情页与根布局共同驱动) */
export const trajectoryReporter = new TrajectoryReporter();
