import AsyncStorage from '@react-native-async-storage/async-storage';

import { GraphQLError, NetworkError } from '@aquarush/common';
import { getOrderDetail } from '@/services/delivery';
import { reportTrajectory } from '@/services/trajectory';
import { Geolocation, PermissionManager } from '@/utils/trajectory-sdk';
import type { GeolocationResponse } from '@/utils/trajectory-sdk';

/**
 * 送水工端配送轨迹上报器(F-ADR-012,R-ADR-03)
 *
 * 定位 SDK:react-native-fitness-geolocation(原生 GPS + 原生 SQLite 持久化,
 * 锁屏/后台/JS 挂起不丢点;Web 端由 trajectory-sdk.web.ts 桩实现替换,自动降级)。
 * - 订单进入「配送中」(开始配送)后自动启动后台持续定位,每 5s 上报一次位置点;
 * - 「完成配送/订单取消」停止上报;App 重启/重新登录后自动恢复配送中订单的轨迹上报;
 * - 弱网上报失败:轨迹点缓存到 AsyncStorage,恢复后补传(不阻塞配送主流程);
 * - 服务端拒绝(非本人/非配送中):停止该订单上报并丢弃其缓存点,避免无限重试;
 * - 上报频率固定 5s(客户端实现,后台可配置为开放问题,见 PRD G65)。
 *
 * 2026-09 恢复实现(原 484b02b 因 Android 编译问题移除 SDK,恢复后已通过构建验证)。
 */
export const REPORT_INTERVAL_MS = 5000;

interface ActiveOrder {
  orderId: string;
  /** 用于 App 重启后 resumeActive 校验订单状态(getOrderDetail 按 orderNo 查询) */
  orderNo?: string;
}

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

/** 本地时间 ISO_LOCAL_DATE_TIME(后端 LocalDateTime 标量格式,如 2026-08-09T12:00:00) */
function toIso(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

class TrajectoryReporter {
  private locationSubscription: { remove(): void } | null = null;
  private activeOrders = new Map<string, ActiveOrder>();
  private lastReportAt: Record<string, number> = {};
  private ensurePromise: Promise<boolean> | null = null;

  /** 当前是否有配送中订单在持续上报 */
  get isActive(): boolean {
    return this.activeOrders.size > 0;
  }

  /**
   * 开始为某订单上报轨迹(订单进入「配送中」时调用;可重复调用,幂等)。
   * @param orderNo 可选:持久化后供 resumeActive 校验订单状态
   * @returns 是否成功启动(权限被拒/定位启动失败返回 false,配送主流程不受影响)
   */
  async startForOrder(orderId: string, orderNo?: string): Promise<boolean> {
    const existing = this.activeOrders.get(orderId);
    if (existing) {
      // 幂等:补全 orderNo(首次调用可能未传入)
      if (orderNo && !existing.orderNo) {
        existing.orderNo = orderNo;
        await this.persistActiveOrders();
      }
      return this.locationSubscription != null;
    }
    this.activeOrders.set(orderId, { orderId, orderNo });
    await this.persistActiveOrders();

    const ok = await this.ensureTracking();
    if (!ok) {
      // 定位权限被拒/启动失败:移除活动订单,避免误报已启动
      this.activeOrders.delete(orderId);
      await this.persistActiveOrders();
    }
    return ok;
  }

  /**
   * 停止为某订单上报轨迹(「完成配送/订单取消」时调用)。
   * 该订单未上报的缓存点直接丢弃(订单已离开 DELIVERING,服务端不再接受,R-ADR-03)。
   */
  async stopForOrder(orderId: string): Promise<void> {
    if (!this.activeOrders.has(orderId)) return;
    this.activeOrders.delete(orderId);
    delete this.lastReportAt[orderId];
    await this.persistActiveOrders();
    await this.dropPendingFor(orderId);
    if (this.activeOrders.size === 0) {
      await this.stopTracking();
    }
  }

  /**
   * 恢复持久化的配送中订单轨迹上报(App 启动/登录成功后调用)。
   * 逐个校验订单状态:仍为 DELIVERING 才恢复;已离开配送中的订单清理停止,
   * 避免对已完成/取消订单持续上报(服务端会拒绝)。
   */
  async resumeActive(): Promise<void> {
    const raw = await AsyncStorage.getItem(ACTIVE_ORDERS_KEY);
    if (!raw) return;
    let records: unknown;
    try {
      records = JSON.parse(raw);
    } catch {
      // 数据损坏:清空,避免阻塞
      await AsyncStorage.removeItem(ACTIVE_ORDERS_KEY);
      return;
    }
    if (!Array.isArray(records) || records.length === 0) return;

    const toResume: ActiveOrder[] = [];
    for (const rec of records) {
      // 兼容旧格式(纯 orderId 字符串数组)与现格式({ orderId, orderNo })
      const orderId = typeof rec === 'string' ? rec : (rec as ActiveOrder)?.orderId;
      if (!orderId) continue;
      if (await this.isDelivering(rec)) {
        toResume.push(typeof rec === 'string' ? { orderId: rec } : (rec as ActiveOrder));
      } else {
        await this.dropPendingFor(orderId);
      }
    }

    if (toResume.length === 0) {
      await AsyncStorage.removeItem(ACTIVE_ORDERS_KEY);
      return;
    }
    for (const rec of toResume) {
      this.activeOrders.set(rec.orderId, rec);
    }
    await this.persistActiveOrders();
    await this.ensureTracking();
  }

  /** 停止全部上报(登出时调用),并清空补传队列(遗留点为已停止订单,服务端不再接受) */
  async stopAll(): Promise<void> {
    this.activeOrders.clear();
    this.lastReportAt = {};
    await this.persistActiveOrders();
    await AsyncStorage.removeItem(PENDING_POINTS_KEY);
    await this.stopTracking();
  }

  // ==================== 内部实现 ====================

  private async isDelivering(rec: unknown): Promise<boolean> {
    const orderNo = typeof rec === 'string' ? undefined : (rec as ActiveOrder)?.orderNo;
    if (!orderNo) {
      // 无 orderNo 无法查询:保守按「继续上报」处理,由服务端拒绝兜底停止
      return true;
    }
    try {
      const res = await getOrderDetail(orderNo);
      return res?.orderByNo?.status === 'DELIVERING';
    } catch (e) {
      // 查询失败(弱网/会话):不阻断恢复,由服务端校验兜底
      console.warn('[trajectory] resumeActive 校验订单状态失败:', e);
      return true;
    }
  }

  private async ensureTracking(): Promise<boolean> {
    // 已在跟踪中:直接返回(多订单并发启动复用同一跟踪,避免重复订阅)
    if (this.locationSubscription) {
      return true;
    }
    // 并发调用复用同一个启动流程
    if (!this.ensurePromise) {
      this.ensurePromise = this.doEnsureTracking().finally(() => {
        this.ensurePromise = null;
      });
    }
    return this.ensurePromise;
  }

  private async doEnsureTracking(): Promise<boolean> {
    // 1. 定位权限(前台 + 后台;运动识别与配送无关,不申请;
    //    includeNotifications: Android 13+ 前台服务通知需 POST_NOTIFICATIONS)
    try {
      const perm = await PermissionManager.requestFitnessPermissions({
        includeMotion: false,
        includeNotifications: true,
      });
      if (perm.status === 'denied' || (!perm.foregroundGranted && !perm.backgroundGranted)) {
        // R-ADR-03:定位权限被拒 → 上报停止,配送主流程不受影响(PRD 14-misc.md §履约类)
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
    for (const orderId of [...this.activeOrders.keys()]) {
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
    } catch (e) {
      if (e instanceof NetworkError) {
        // 弱网失败:缓存轨迹点,恢复后补传(不阻塞配送主流程)
        await this.enqueuePending({ orderId, longitude, latitude, accuracy, reportedAt: toIso(at) });
      } else if (e instanceof GraphQLError) {
        // 服务端拒绝(非本人/非配送中,如订单已完成):停止该订单并丢弃其缓存点,
        // 避免无限重试;状态机离开 DELIVERING 后不会返回,停止是安全的。
        console.warn(`[trajectory] 服务端拒绝订单 ${orderId} 轨迹上报,停止上报:`, e.message);
        await this.stopForOrder(orderId);
      } else {
        // 未知错误(桥接/其他):按弱网处理,缓存补传
        await this.enqueuePending({ orderId, longitude, latitude, accuracy, reportedAt: toIso(at) });
      }
    }
  }

  // ==================== AsyncStorage 持久化 ====================

  private async persistActiveOrders(): Promise<void> {
    const records = [...this.activeOrders.values()];
    if (records.length === 0) {
      await AsyncStorage.removeItem(ACTIVE_ORDERS_KEY);
    } else {
      await AsyncStorage.setItem(ACTIVE_ORDERS_KEY, JSON.stringify(records));
    }
  }

  private async readPending(): Promise<PendingPoint[]> {
    try {
      const raw = await AsyncStorage.getItem(PENDING_POINTS_KEY);
      return raw ? (JSON.parse(raw) as PendingPoint[]) : [];
    } catch {
      return [];
    }
  }

  private async writePending(points: PendingPoint[]): Promise<void> {
    if (points.length === 0) {
      await AsyncStorage.removeItem(PENDING_POINTS_KEY);
    } else {
      await AsyncStorage.setItem(PENDING_POINTS_KEY, JSON.stringify(points));
    }
  }

  private async enqueuePending(point: PendingPoint): Promise<void> {
    const points = await this.readPending();
    points.push(point);
    // 限制缓存上限,防止无限增长
    if (points.length > MAX_PENDING_POINTS) {
      points.splice(0, points.length - MAX_PENDING_POINTS);
    }
    await this.writePending(points);
  }

  /** 丢弃某订单的全部缓存点(订单已离开配送中,服务端不再接受) */
  private async dropPendingFor(orderId: string): Promise<void> {
    const points = await this.readPending();
    if (points.length === 0) return;
    const remaining = points.filter((p) => p.orderId !== orderId);
    await this.writePending(remaining);
  }

  /** 逐条补传缓存点;网络失败保留(下次成功再补),服务端拒绝/其他错误丢弃 */
  private async flushPending(): Promise<void> {
    const points = await this.readPending();
    if (points.length === 0) return;
    const remaining: PendingPoint[] = [];
    for (const p of points) {
      try {
        await reportTrajectory(p.orderId, p.longitude, p.latitude, p.accuracy, p.reportedAt);
      } catch (e) {
        if (e instanceof NetworkError) {
          remaining.push(p);
        }
        // GraphQLError/未知:服务端拒绝(订单已非配送中)或不可恢复,丢弃该点
      }
    }
    await this.writePending(remaining);
  }
}

/** 单例:全 App 共享一个轨迹上报器(由订单详情页与根布局共同驱动) */
export const trajectoryReporter = new TrajectoryReporter();
