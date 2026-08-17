/**
 * 送水工端配送轨迹上报器(F-ADR-012,R-ADR-03)——已暂停
 *
 * 原实现基于 react-native-fitness-geolocation(原生 GPS + SQLite 持久化)持续
 * 上报配送轨迹;该 SDK 已移除,轨迹上报功能整体置空。
 * 本类保留原公共 API(startForOrder / stopForOrder / resumeActive / stopAll /
 * isActive),所有方法均为空实现(不启动定位、不缓存、不上报),调用方无需改动。
 * 后续接入替代定位 SDK 时,在此恢复实现即可。
 */
export const REPORT_INTERVAL_MS = 5000;

class TrajectoryReporter {
  /** 当前是否有配送中订单在持续上报(置空后恒为 false) */
  get isActive(): boolean {
    return false;
  }

  /** 开始为某订单上报轨迹(空实现,恒返回 false 表示未启动) */
  async startForOrder(_orderId: string): Promise<boolean> {
    return false;
  }

  /** 停止为某订单上报轨迹(空实现) */
  async stopForOrder(_orderId: string): Promise<void> {}

  /** 恢复持久化的配送中订单轨迹上报(空实现) */
  async resumeActive(): Promise<void> {}

  /** 停止全部上报(空实现) */
  async stopAll(): Promise<void> {}
}

/** 单例:全 App 共享一个轨迹上报器(由订单详情页与根布局共同驱动) */
export const trajectoryReporter = new TrajectoryReporter();
