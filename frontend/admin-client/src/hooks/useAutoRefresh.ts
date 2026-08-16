import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * 数据统计页面「定时更新」可选间隔(毫秒),value 0 表示关闭。
 * 与 RefreshControls 组件共享,保证下拉选项与 hook 语义一致。
 */
export const REFRESH_INTERVAL_OPTIONS: Array<{ value: number; label: string }> = [
  { value: 0, label: '关闭' },
  { value: 30000, label: '30 秒' },
  { value: 60000, label: '1 分钟' },
  { value: 300000, label: '5 分钟' },
];

export type RefreshInterval = number | null; // null 表示关闭定时更新

interface UseAutoRefreshOptions {
  /**
   * 刷新回调:重新拉取当前页数据(通常为活跃 useQuery 的 refetch)。
   * 使用 unknown 以兼容 Apollo refetch 返回的 Promise<ApolloQueryResult<T>>。
   */
  onRefresh: () => unknown;
  /** 默认间隔,默认 null(关闭) */
  defaultInterval?: RefreshInterval;
}

interface UseAutoRefreshResult {
  /** 当前定时更新间隔(毫秒),null 表示关闭 */
  intervalMs: RefreshInterval;
  /** 修改定时更新间隔;传 null 关闭 */
  setIntervalMs: (ms: RefreshInterval) => void;
  /** 手动刷新是否进行中(用于刷新按钮 loading) */
  refreshing: boolean;
  /** 手动刷新:await 回调后更新上次刷新时间 */
  refresh: () => Promise<void>;
  /** 最近一次成功刷新时间(手动或自动),尚未刷新过为 null */
  lastUpdated: Date | null;
}

/**
 * 数据统计页通用「手动刷新 + 定时更新」Hook:
 * - onRefresh 存 ref,定时器闭包始终调用最新回调,避免过期闭包;
 * - 自动刷新不触发 refreshing(避免页面周期性闪烁 loading),仅更新 lastUpdated;
 * - in-flight 标志防止手动与定时刷新重叠;
 * - 定时器随 intervalMs 变化重建,卸载时自动清理。
 */
export function useAutoRefresh({ onRefresh, defaultInterval = null }: UseAutoRefreshOptions): UseAutoRefreshResult {
  const [intervalMs, setIntervalMs] = useState<RefreshInterval>(defaultInterval);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const onRefreshRef = useRef(onRefresh);
  onRefreshRef.current = onRefresh;
  const inFlightRef = useRef(false);

  const runRefresh = useCallback(async (isManual: boolean) => {
    if (inFlightRef.current) {
      return;
    }
    inFlightRef.current = true;
    if (isManual) {
      setRefreshing(true);
    }
    try {
      await onRefreshRef.current();
      setLastUpdated(new Date());
    } catch {
      // 刷新失败由页面既有 useQuery error 状态展示(各页已有错误提示/重试),
      // 这里仅避免手动刷新按钮产生未处理的 Promise rejection
    } finally {
      inFlightRef.current = false;
      if (isManual) {
        setRefreshing(false);
      }
    }
  }, []);

  const refresh = useCallback(() => runRefresh(true), [runRefresh]);

  useEffect(() => {
    if (intervalMs == null || intervalMs <= 0) {
      return;
    }
    const timer = setInterval(() => {
      void runRefresh(false);
    }, intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs, runRefresh]);

  return { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated };
}
