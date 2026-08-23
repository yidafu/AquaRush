import { useCallback, useRef, useState } from 'react';

export interface UseDebouncedFiltersOptions<T extends object> {
  /** 初始值;reset() 也重置回该值 */
  initial: T;
  /** 防抖毫秒(默认 300);仅对非 immediate 的更新生效 */
  debounceMs?: number;
  /** 父组件接收最终过滤条件 */
  onFiltersChange?: (filters: T) => void;
}

export interface UseDebouncedFiltersResult<T extends object> {
  /** 本地(受控输入源)过滤条件 */
  filters: T;
  /** 更新过滤条件;opts.immediate=true 立即回调,否则按 debounceMs 防抖 */
  setFilters: (next: T, opts?: { immediate?: boolean }) => void;
  /** 重置回 initial 并立即回调 */
  reset: () => void;
}

/**
 * 筛选表单本地状态 + 300ms 防抖回调样板(OrderFilters/ProductFilters 等共用)。
 * - 输入类字段(关键词)走防抖;
 * - 选择类字段(状态/日期/数值)经 opts.immediate 立即回调。
 */
export function useDebouncedFilters<T extends object>(
  options: UseDebouncedFiltersOptions<T>,
): UseDebouncedFiltersResult<T> {
  const { initial, debounceMs = 300, onFiltersChange } = options;
  const [filters, setLocalFilters] = useState<T>(() => ({ ...initial }));

  // 回调/防抖定时器放 ref,避免组件重渲染导致定时器重建
  const onChangeRef = useRef(onFiltersChange);
  onChangeRef.current = onFiltersChange;
  const timeoutRef = useRef<number | undefined>(undefined);

  const clearTimer = useCallback(() => {
    if (timeoutRef.current !== undefined) {
      window.clearTimeout(timeoutRef.current);
      timeoutRef.current = undefined;
    }
  }, []);

  const setFilters = useCallback(
    (next: T, opts?: { immediate?: boolean }) => {
      setLocalFilters(next);
      clearTimer();
      if (opts?.immediate) {
        onChangeRef.current?.(next);
        return;
      }
      timeoutRef.current = window.setTimeout(() => {
        onChangeRef.current?.(next);
      }, debounceMs);
    },
    [clearTimer, debounceMs],
  );

  const reset = useCallback(() => {
    clearTimer();
    const empty = { ...initial };
    setLocalFilters(empty);
    onChangeRef.current?.(empty);
  }, [clearTimer, initial]);

  return { filters, setFilters, reset };
}
