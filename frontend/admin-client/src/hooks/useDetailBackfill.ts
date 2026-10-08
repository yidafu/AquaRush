import { useEffect, useRef } from 'react';

export interface UseDetailBackfillParams<T extends { id: string | number }> {
  open: boolean;
  /** 编辑对象;null = 新建 */
  record: T | null;
  /** 编辑模式打开后按 ID 拉取详情(如 loadBanner({ variables: { id } })) */
  load: (id: string) => void;
  /** 详情查询结果(未加载时为 undefined) */
  detail: T | null | undefined;
  /** 详情查询错误(非 null 时退化为用列表记录回填) */
  detailError?: unknown;
  /** 新建模式(record=null)打开时回填默认值 */
  applyDefaults?: () => void;
  /** 详情就绪(或失败回退)后回填表单;字段映射保留在调用方 */
  backfill: (source: T) => void;
}

export interface UseDetailBackfillResult<T extends { id: string | number }> {
  /** 编辑模式且详情尚未返回(且未失败)时为 false,此时应禁用提交 */
  ready: boolean;
  /** 回填数据源:详情(与记录 ID 匹配)优先;查询失败退化为列表记录;未就绪/新建为 null */
  source: T | null;
}

/**
 * banner/notice 类弹窗的「编辑按 ID 拉详情回填 + detailReady 禁提交」模式:
 * - 打开编辑时按 record.id 触发 load;
 * - 详情返回后(或查询失败)经 backfill 回填表单,新建模式经 applyDefaults 给默认值;
 * - ready=false 期间确定按钮禁用。
 *
 * 字段映射与 payload 转换仍保留在各弹窗组件内。
 */
export function useDetailBackfill<T extends { id: string | number }>(
  params: UseDetailBackfillParams<T>,
): UseDetailBackfillResult<T> {
  const { open, record, load, detail, detailError, applyDefaults, backfill } = params;

  // 用 ref 持有回调,避免 useEffect 依赖变化反复触发
  const loadRef = useRef(load);
  const defaultsRef = useRef(applyDefaults);
  const backfillRef = useRef(backfill);
  loadRef.current = load;
  defaultsRef.current = applyDefaults;
  backfillRef.current = backfill;

  // 编辑模式:打开后按 ID 拉取详情
  useEffect(() => {
    if (open && record) {
      loadRef.current(String(record.id));
    }
  }, [open, record]);

  // 数据源:详情(与记录 ID 匹配)优先;查询失败退化为列表记录;新建/未就绪 = null
  const source: T | null = !record
    ? null
    : detail && detail.id === record.id
      ? detail
      : detailError
        ? record
        : null;

  // 详情就绪(或失败)后回填;新建模式给默认值
  useEffect(() => {
    if (!open) return;
    if (!record) {
      defaultsRef.current?.();
      return;
    }
    if (source) {
      backfillRef.current(source);
    }
  }, [open, record, source]);

  // 编辑模式且详情尚未返回(且未失败)时不可提交
  const ready = !record || !!detailError || (!!detail && detail.id === record.id);

  return { ready, source };
}
