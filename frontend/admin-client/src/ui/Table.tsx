/**
 * Table —— antd 形状表壳(TanStack 核心)。
 * 契约: docs/compose/spec/admin-radix-ui.md [S2.5]
 *  - 列编译与排序状态交由 @tanstack/react-table(列定义 / sorting state / 表头切换);
 *  - 本地 sorter 函数在表壳内排序当前 dataSource;sorter:true 走服务端(仅发 onChange);
 *  - 分页为受控/手动模式:pagination.onChange 与 Table onChange 回调上报,数据获取在调用方;
 *  - 行选择完全由调用方持有 selectedRowKeys,表壳只做勾选交互与回调。
 */
import { useEffect, useMemo, useRef } from 'react';
import type { HTMLAttributes, Key, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import {
  rowSortingFeature,
  tableFeatures,
  useTable,
  type ColumnDef,
  type RowData,
  type SortingState,
} from '@tanstack/react-table';
import { ArrowDownOutlined, ArrowUpOutlined } from './icons';
import { Empty } from './Empty';
import { Pagination, type PaginationShowTotal } from './Pagination';
import {
  applySort,
  paginationSlice,
  resolveDataIndex,
  resolveRowKey,
  toggleAll,
  toggleKey,
  type DataIndex,
  type RowKey,
} from './table-core';

// ---------------------------------------------------------------------------
// 对外类型(antd 兼容形状)
// ---------------------------------------------------------------------------

/** antd sorter 结果(表壳经 Table onChange 上报,服务端排序依赖它)。 */
export interface SorterResult<T> {
  column?: Column<T>;
  /** antd 的 field = 列 dataIndex(字符串或嵌套路径数组)。 */
  field?: DataIndex;
  columnKey?: string;
  order: 'ascend' | 'descend' | null;
}

/** antd Table onChange 的 filters 参数(本表壳不实现列筛选,恒为 {})。 */
export type TableFiltersValue = Record<string, (string | number | boolean)[] | null>;

/** antd Table onChange 第 4 参(与 antd 对齐,调用方当前未使用)。 */
export interface TableChangeExtra<T> {
  currentDataSource: T[];
  action: 'paginate' | 'sort' | 'filter';
}

/** antd TablePaginationConfig 子集(覆盖全部 49 个调用点的 pagination 形状)。 */
export interface TablePaginationConfig {
  current?: number;
  pageSize?: number;
  total?: number;
  disabled?: boolean;
  hideOnSinglePage?: boolean;
  showSizeChanger?: boolean;
  showQuickJumper?: boolean;
  showTitle?: boolean;
  simple?: boolean;
  // size 不含 'large':与 antd TablePaginationConfig(extends PaginationProps)兼容,
  // 迁移期 utils/table.ts 工厂的返回值会同时喂给尚未迁移的 antd Table。
  size?: 'default' | 'small';
  pageSizeOptions?: (string | number)[];
  // 与 PaginationProps.showTotal 同宽(boolean 开关 + 函数形态),
  // PaginationProps 可直接喂给 <Table pagination> 免断言
  showTotal?: PaginationShowTotal;
  onChange?: (page: number, pageSize: number) => void;
  onShowSizeChange?: (current: number, size: number) => void;
}

/** antd rowSelection 子集(orders 使用 selectedRowKeys/onChange/getCheckboxProps)。 */
export interface TableRowSelectionInfo {
  type: 'all' | 'none' | 'one' | 'some';
}

export interface TableRowSelection<T> {
  selectedRowKeys: Key[];
  onChange?: (selectedRowKeys: Key[], selectedRows: T[], info: TableRowSelectionInfo) => void;
  /** 跨页保留已选键:表壳不持有选择状态,调用方不重置即天然保留。 */
  preserveSelectedRowKeys?: boolean;
  disabled?: boolean;
  getCheckboxProps?: (record: T) => { disabled?: boolean };
}

/** antd 列定义(实际调用点用到的字段全集,见 spec S2.5)。 */
export interface Column<T> {
  title?: ReactNode;
  dataIndex?: DataIndex;
  key?: string;
  // antd render 首参为 any:调用点普遍写 (value: someType) 形参,须保持 any 双变兼容
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  render?: (value: any, record: T, index: number) => ReactNode;
  /** 函数型 → 表壳内本地排序;true → 服务端排序(仅上报 onChange);缺省 → 不可排序。 */
  sorter?: ((a: T, b: T) => number) | boolean;
  /** 初始排序提示(仅初始化 TanStack sorting 状态,非受控)。 */
  sortOrder?: 'ascend' | 'descend' | null;
  align?: 'left' | 'right' | 'center';
  width?: number | string;
  fixed?: 'left' | 'right';
  ellipsis?: boolean;
}

/** antd ColumnsType 别名 —— 40+ 调用点 `import type { ColumnsType } from 'antd/es/table'` 迁移用。 */
export type ColumnsType<T> = Column<T>[];

export interface TableProps<T> {
  columns: Column<T>[];
  dataSource: readonly T[];
  rowKey?: RowKey<T>;
  loading?: boolean | { spinning?: boolean };
  bordered?: boolean;
  size?: 'large' | 'middle' | 'small';
  title?: ReactNode | ((data: readonly T[]) => ReactNode);
  footer?: ReactNode | ((data: readonly T[]) => ReactNode);
  /** false = 不分页(全量展示);缺省按 antd 语义启用默认分页。 */
  pagination?: TablePaginationConfig | false;
  onChange?: (
    pagination: TablePaginationConfig,
    filters: TableFiltersValue,
    sorter: SorterResult<T> | SorterResult<T>[],
    extra?: TableChangeExtra<T>,
  ) => void;
  onRow?: (record: T, index: number) => HTMLAttributes<HTMLElement>;
  rowClassName?: string | ((record: T, index: number) => string);
  rowSelection?: TableRowSelection<T>;
  scroll?: { x?: number | string; y?: number | string };
  locale?: { emptyText?: ReactNode };
}

// ---------------------------------------------------------------------------
// 样式(StyleX:全 longhand;token 来自 @aquarush/common/theme/tokens.stylex)
// ---------------------------------------------------------------------------

const SELECT_COLUMN_WIDTH = 48;

// 行悬停色 rgba(58,155,217,0.10) 叠白底的实色等效:
// sticky 单元格必须不透明(否则透出其下滚动内容),半透明 tint 无法直接使用
const STICKY_HOVER_SOLID = '#EBF5FB';
// loading 蒙层半透明白(design token 无 alpha 白字面量,保留字面量并在交付报告列出)
const LOADING_MASK = 'rgba(255, 255, 255, 0.6)';

const spinKeyframes = stylex.keyframes({
  from: { transform: 'rotate(0deg)' },
  to: { transform: 'rotate(360deg)' },
});

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: tokens.card,
    fontFamily: tokens.font,
    fontSize: '14px',
    color: tokens.text,
  },
  rootBordered: {
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: tokens.border,
    borderRadius: tokens.rSm,
    overflow: 'hidden',
  },
  title: {
    paddingTop: '16px',
    paddingRight: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
    fontSize: '16px',
    fontWeight: '600',
  },
  footer: {
    paddingTop: '8px',
    paddingRight: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
    color: tokens.textSub,
  },
  bodyWrap: {
    position: 'relative',
  },
  scroll: {
    overflowX: 'auto',
    overflowY: 'auto',
    // maxHeight 由 scroll.y 动态注入(style={{}})
  },
  table: {
    width: '100%',
    borderCollapse: 'separate',
    borderSpacing: '0px',
    backgroundColor: tokens.card,
    // width/minWidth 由 scroll.x 动态注入(style={{}})
  },
  tableFixed: {
    tableLayout: 'fixed',
  },
  // —— 表头单元格(默认 middle 尺寸) ——
  th: {
    backgroundColor: tokens.bg,
    color: tokens.text,
    fontWeight: '600',
    fontSize: '14px',
    lineHeight: '20px',
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '16px',
    paddingRight: '16px',
    textAlign: 'left',
    verticalAlign: 'middle',
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
    whiteSpace: 'nowrap',
  },
  // —— 数据单元格 ——
  td: {
    fontSize: '14px',
    lineHeight: '20px',
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '16px',
    paddingRight: '16px',
    textAlign: 'left',
    verticalAlign: 'middle',
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
    color: tokens.text,
  },
  cellSmall: {
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    paddingRight: '8px',
  },
  cellLarge: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  alignRight: {
    textAlign: 'right',
  },
  alignCenter: {
    textAlign: 'center',
  },
  thInner: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '4px',
    minWidth: '0px',
  },
  thInnerEnd: {
    justifyContent: 'flex-end',
  },
  thSortable: {
    cursor: 'pointer',
  },
  sortIcons: {
    display: 'inline-flex',
    alignItems: 'center',
    columnGap: '2px',
    fontSize: '10px',
    lineHeight: '1',
    flexShrink: '0',
  },
  sortIconMuted: {
    color: tokens.textWeak,
  },
  sortIconActive: {
    color: tokens.brand,
  },
  // —— 行:hover tint(设计规范品牌浅底,等同原 .ant-table-row.clickable-row:hover) ——
  row: {
    ':hover td': {
      backgroundColor: tokens.brandTint,
    },
    // sticky 单元格悬停:保持不透明实色(见 STICKY_HOVER_SOLID 说明);
    // 选择器特异度(0,3,0) 高于 ':hover td'(0,2,1),可覆盖上面的半透明 tint
    ':hover .stickyCell': {
      backgroundColor: STICKY_HOVER_SOLID,
    },
  },
  rowClickable: {
    cursor: 'pointer',
  },
  // sticky(固定列)单元格:不透明底 + 层级,offset 走 style 动态注入
  stickyCell: {
    position: 'sticky',
    zIndex: '1',
    backgroundColor: tokens.card,
  },
  // scroll.y 时表头吸顶
  thTop: {
    position: 'sticky',
    top: '0px',
    zIndex: '3',
    backgroundColor: tokens.bg,
  },
  // 固定列表头(无 scroll.y)
  thFixed: {
    position: 'sticky',
    zIndex: '2',
  },
  // 固定列表头(有 scroll.y,吸顶 + 横向固定)
  thFixedTop: {
    position: 'sticky',
    top: '0px',
    zIndex: '4',
    backgroundColor: tokens.bg,
  },
  ellipsisInner: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  selHeader: {
    textAlign: 'center',
  },
  checkbox: {
    width: '14px',
    height: '14px',
    cursor: 'pointer',
    accentColor: tokens.brand,
    verticalAlign: 'middle',
  },
  emptyCell: {
    paddingTop: '40px',
    paddingBottom: '40px',
    textAlign: 'center',
  },
  loadingMask: {
    position: 'absolute',
    top: '0px',
    right: '0px',
    bottom: '0px',
    left: '0px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: '8px',
    zIndex: '10',
    backgroundColor: LOADING_MASK,
  },
  spinner: {
    width: '24px',
    height: '24px',
    boxSizing: 'border-box',
    borderWidth: '2px',
    borderStyle: 'solid',
    borderColor: tokens.brandTint,
    borderTopColor: tokens.brand,
    borderRadius: '50%',
    animationName: spinKeyframes,
    animationDuration: '0.8s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
  loadingTip: {
    fontSize: '14px',
    color: tokens.textSub,
  },
  pagerWrap: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    paddingTop: '12px',
    paddingRight: '16px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    columnGap: '8px',
  },
});

const alignStyle = {
  left: undefined,
  right: styles.alignRight,
  center: styles.alignCenter,
} as const;

// ---------------------------------------------------------------------------
// 内部工具
// ---------------------------------------------------------------------------

const tableFeaturesSet = tableFeatures({ rowSortingFeature });
type TableFeatureSet = typeof tableFeaturesSet;

function columnIdOf<T>(column: Column<T>, index: number): string {
  if (column.key !== undefined) return column.key;
  if (typeof column.dataIndex === 'string') return column.dataIndex;
  return `__col_${index}`;
}

function widthPx(width: number | string | undefined): number {
  // sticky 偏移按声明宽度累加;调用点固定列均带显式数字宽度,缺省按 0 计(近似)
  if (typeof width === 'number') return width;
  if (typeof width === 'string') {
    const parsed = Number.parseFloat(width);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

/** 从 ReactNode 提取纯文本(ellipsis 单元格的 title 提示),元素递归取 children。 */
function textOf(node: ReactNode): string | undefined {
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) {
    const parts = node.map((child) => textOf(child)).filter((part): part is string => part !== undefined);
    return parts.length > 0 ? parts.join('') : undefined;
  }
  if (node !== null && typeof node === 'object' && 'props' in node) {
    const children = (node as { props?: { children?: ReactNode } }).props?.children;
    if (children !== undefined) return textOf(children);
  }
  return undefined;
}

// ---------------------------------------------------------------------------
// Table 组件
// ---------------------------------------------------------------------------

export function Table<T extends RowData>(props: TableProps<T>) {
  const {
    columns,
    dataSource,
    rowKey,
    loading,
    bordered,
    size,
    title,
    footer,
    pagination,
    onChange,
    onRow,
    rowClassName,
    rowSelection,
    scroll,
    locale,
  } = props;

  // —— 列编译(TanStack 列定义 + id → antd Column 索引) ——
  const compiled = useMemo(() => {
    const byId = new Map<string, Column<T>>();
    const defs: Array<ColumnDef<TableFeatureSet, T>> = columns.map((column, index) => {
      const id = columnIdOf(column, index);
      byId.set(id, column);
      const sortable = column.sorter !== undefined && column.sorter !== false;
      const base = {
        id,
        header: () => column.title,
        enableSorting: sortable,
        // 强制升序起步(antd 排序周期:ascend → descend → 无)
        sortDescFirst: false,
      };
      if (typeof column.dataIndex === 'string') {
        return { ...base, accessorKey: column.dataIndex };
      }
      if (column.dataIndex !== undefined) {
        const path = column.dataIndex;
        return { ...base, accessorFn: (row: T) => resolveDataIndex(row, path) };
      }
      return base;
    });
    return { defs, byId };
  }, [columns]);

  // —— 初始排序(column.sortOrder 提示,仅首个生效) ——
  const initialSorting = useMemo<SortingState>(() => {
    const index = columns.findIndex((c) => c.sortOrder === 'ascend' || c.sortOrder === 'descend');
    if (index < 0) return [];
    return [{ id: columnIdOf(columns[index], index), desc: columns[index].sortOrder === 'descend' }];
  }, [columns]);

  // —— TanStack 实例(排序状态内部持有;分页为手动模式,数据获取在调用方) ——
  const table = useTable<TableFeatureSet, T>({
    features: tableFeaturesSet,
    columns: compiled.defs,
    data: dataSource,
    getRowId: (row, index) => String(resolveRowKey(row, index, rowKey)),
    enableMultiSort: false,
    manualSorting: true,
    initialState: { sorting: initialSorting },
  });
  const sorting: SortingState = table.state.sorting;

  // —— 本地排序:函数型 sorter 在表壳内排序当前 dataSource;sorter:true 保持返回序 ——
  const sortedRows = (() => {
    const entry = sorting[0];
    if (!entry) return table.getRowModel().rows;
    const column = compiled.byId.get(entry.id);
    const sorter = column && typeof column.sorter === 'function' ? column.sorter : undefined;
    if (!sorter) return table.getRowModel().rows;
    const rows = table.getRowModel().rows;
    const cmp = (a: (typeof rows)[number], b: (typeof rows)[number]) => sorter(a.original, b.original);
    return applySort(rows, entry.desc ? (a, b) => cmp(b, a) : cmp);
  })();

  // —— 分页配置与切片(antd 对齐:服务端单页数据不再切片) ——
  const pag: TablePaginationConfig | null =
    pagination === false
      ? null
      : { current: 1, pageSize: 10, total: dataSource.length, ...(pagination ?? {}) };
  const current = pag?.current ?? 1;
  const pageSize = pag?.pageSize ?? 10;
  const total = pag?.total ?? dataSource.length;
  const { pageRows } = pag
    ? paginationSlice(sortedRows, pag.current, pag.pageSize, total)
    : { pageRows: sortedRows };

  // —— sorter 构造(antd onChange 第 3 参;清空排序时保留上次列的 field) ——
  const lastSortedIdRef = useRef<string | null>(null);
  const buildSorter = (state: SortingState): SorterResult<T> => {
    const entry = state[0];
    if (entry) lastSortedIdRef.current = entry.id;
    const activeId = entry ? entry.id : lastSortedIdRef.current;
    const column = activeId !== null ? compiled.byId.get(activeId) : undefined;
    if (!column) return { order: null };
    return {
      column,
      field: column.dataIndex,
      columnKey: column.key,
      order: entry ? (entry.desc ? 'descend' : 'ascend') : null,
    };
  };

  const emitChange = (
    nextPagination: TablePaginationConfig,
    state: SortingState,
    action: 'paginate' | 'sort',
  ) => {
    onChange?.(
      nextPagination,
      {},
      buildSorter(state),
      { currentDataSource: sortedRows.map((row) => row.original), action },
    );
  };

  const handlePageChange = (page: number, size: number) => {
    if (!pag) return;
    pag.onChange?.(page, size);
    emitChange({ ...pag, current: page, pageSize: size }, sorting, 'paginate');
  };

  // —— 行选择(选择键由调用方持有;表头全选针对当前页行) ——
  const selectedSet = new Set(rowSelection?.selectedRowKeys ?? []);
  const pageMeta = pageRows.map((row) => {
    const key = resolveRowKey(row.original, row.index, rowKey);
    const disabled = Boolean(
      rowSelection?.disabled || rowSelection?.getCheckboxProps?.(row.original)?.disabled,
    );
    return { row, key, disabled };
  });
  const selectableKeys: Key[] = pageMeta.filter((m) => !m.disabled).map((m) => m.key);
  const selectedSelectableCount = selectableKeys.filter((k) => selectedSet.has(k)).length;
  const allChecked = selectableKeys.length > 0 && selectedSelectableCount === selectableKeys.length;
  const someChecked = selectedSelectableCount > 0 && !allChecked;

  const selectAllRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (selectAllRef.current) selectAllRef.current.indeterminate = someChecked;
  }, [someChecked]);

  const emitSelection = (nextKeys: Key[], info: TableRowSelectionInfo) => {
    if (!rowSelection?.onChange) return;
    const nextSet = new Set(nextKeys);
    const rowsOfKeys = pageMeta.filter((m) => nextSet.has(m.key)).map((m) => m.row.original);
    rowSelection.onChange(nextKeys, rowsOfKeys, info);
  };

  const handleRowToggle = (key: Key) => {
    if (!rowSelection) return;
    emitSelection(toggleKey(rowSelection.selectedRowKeys, key), { type: 'one' });
  };

  const handleSelectAll = (checked: boolean) => {
    if (!rowSelection) return;
    const next = checked
      ? toggleAll(selectableKeys, rowSelection.selectedRowKeys, true)
      : toggleAll(pageMeta.map((m) => m.key), rowSelection.selectedRowKeys, false);
    emitSelection(next, { type: checked ? 'all' : 'none' });
  };

  // —— 表头排序切换:先走 TanStack toggle,再读取实时 sorting 上报 onChange ——
  const handleSortToggle = (
    toggle: ((event: unknown) => void) | undefined,
    event: React.MouseEvent,
  ) => {
    if (!toggle) return;
    toggle(event);
    if (onChange) emitChange({ ...(pag ?? {}) }, table.atoms.sorting.get(), 'sort');
  };

  // —— 固定列 sticky 偏移(按声明宽度累加,选择列计入左侧) ——
  const leftOffsets: number[] = [];
  {
    let acc = rowSelection ? SELECT_COLUMN_WIDTH : 0;
    for (const column of columns) {
      leftOffsets.push(acc);
      acc += widthPx(column.width);
    }
  }
  const rightOffsets: number[] = new Array(columns.length).fill(0);
  {
    let acc = 0;
    for (let i = columns.length - 1; i >= 0; i -= 1) {
      rightOffsets[i] = acc;
      acc += widthPx(columns[i].width);
    }
  }

  // —— 布局(对齐 rc-table:固定列/表头吸顶/ellipsis 列 → table-layout: fixed) ——
  const hasFixedColumn = columns.some((c) => c.fixed !== undefined);
  const hasEllipsis = columns.some((c) => c.ellipsis === true);
  const needFixedLayout =
    (scroll?.x != null && hasFixedColumn) || scroll?.y != null || hasEllipsis;

  // —— 尺寸与加载/空态 ——
  const cellPadClass = size === 'small' ? styles.cellSmall : size === 'large' ? styles.cellLarge : undefined;
  const isLoading =
    loading === true || (typeof loading === 'object' && loading !== null && loading.spinning !== false);
  const emptyText = locale?.emptyText;
  const emptyNode =
    emptyText === undefined ? (
      <Empty description="暂无数据" />
    ) : typeof emptyText === 'string' ? (
      <Empty description={emptyText} />
    ) : (
      emptyText
    );

  const headers = table.getHeaderGroups()[0]?.headers ?? [];
  const totalColumnCount = columns.length + (rowSelection ? 1 : 0);

  const renderHeaderCell = (header: (typeof headers)[number], index: number) => {
    const column = columns[index];
    const alignCls = alignStyle[column?.align ?? 'left'];
    const canSort = header.column.getCanSort();
    const sorted = header.column.getIsSorted();
    const toggle = canSort ? header.column.getToggleSortingHandler() : undefined;
    const stickyCls = column?.fixed !== undefined
      ? scroll?.y != null
        ? styles.thFixedTop
        : styles.thFixed
      : scroll?.y != null
        ? styles.thTop
        : undefined;
    const stickyStyle =
      column?.fixed === 'left'
        ? { left: leftOffsets[index] }
        : column?.fixed === 'right'
          ? { right: rightOffsets[index] }
          : undefined;
    return (
      <th
        key={header.id}
        scope="col"
        {...stylex.props(
          styles.th,
          cellPadClass,
          alignCls,
          canSort && styles.thSortable,
          stickyCls,
        )}
        style={stickyStyle}
        aria-sort={
          canSort ? (sorted === 'asc' ? 'ascending' : sorted === 'desc' ? 'descending' : 'none') : undefined
        }
        onClick={toggle ? (event) => handleSortToggle(toggle, event) : undefined}
      >
        <div {...stylex.props(styles.thInner, (column?.align ?? 'left') === 'right' && styles.thInnerEnd)}>
          <span>
            {/* TanStack header 模板渲染(编译期已包装为 column.title) */}
            <table.FlexRender header={header} />
          </span>
          {canSort && (
            <span {...stylex.props(styles.sortIcons)}>
              {sorted === 'asc' ? (
                <ArrowUpOutlined {...stylex.props(styles.sortIconActive)} />
              ) : sorted === 'desc' ? (
                <ArrowDownOutlined {...stylex.props(styles.sortIconActive)} />
              ) : (
                <>
                  <ArrowUpOutlined {...stylex.props(styles.sortIconMuted)} />
                  <ArrowDownOutlined {...stylex.props(styles.sortIconMuted)} />
                </>
              )}
            </span>
          )}
        </div>
      </th>
    );
  };

  const renderBodyCell = (row: (typeof pageRows)[number], column: Column<T>, index: number) => {
    const record = row.original;
    const value = resolveDataIndex(record, column.dataIndex);
    const content = column.render
      ? column.render(value, record, row.index)
      : column.dataIndex !== undefined
        ? (value as ReactNode)
        : null;
    const alignCls = alignStyle[column.align ?? 'left'];
    const stickyCls = column.fixed !== undefined ? styles.stickyCell : undefined;
    const stickyStyle =
      column.fixed === 'left'
        ? { left: leftOffsets[index] }
        : column.fixed === 'right'
          ? { right: rightOffsets[index] }
          : undefined;
    return (
      <td
        key={index}
        {...stylex.props(styles.td, cellPadClass, alignCls, stickyCls)}
        style={stickyStyle}
        title={column.ellipsis ? textOf(content) : undefined}
      >
        {column.ellipsis ? (
          <div {...stylex.props(styles.ellipsisInner)}>{content}</div>
        ) : (
          content
        )}
      </td>
    );
  };

  return (
    <div {...stylex.props(styles.root, bordered && styles.rootBordered)}>
      {title !== undefined && (
        <div {...stylex.props(styles.title)}>
          {typeof title === 'function' ? title(dataSource) : title}
        </div>
      )}

      <div {...stylex.props(styles.bodyWrap)}>
        <div
          {...stylex.props(styles.scroll)}
          style={scroll?.y != null ? { maxHeight: scroll.y } : undefined}
        >
          <table
            {...stylex.props(styles.table, needFixedLayout && styles.tableFixed)}
            style={scroll?.x != null ? { width: scroll.x, minWidth: '100%' } : undefined}
          >
            <colgroup>
              {rowSelection && <col style={{ width: SELECT_COLUMN_WIDTH }} />}
              {columns.map((column, index) => (
                <col key={columnIdOf(column, index)} style={{ width: column.width }} />
              ))}
            </colgroup>
            <thead>
              <tr>
                {rowSelection && (
                  <th
                    scope="col"
                    {...stylex.props(styles.th, cellPadClass, styles.selHeader, scroll?.y != null && styles.thTop)}
                  >
                    <input
                      ref={selectAllRef}
                      type="checkbox"
                      {...stylex.props(styles.checkbox)}
                      checked={allChecked}
                      disabled={selectableKeys.length === 0}
                      onChange={(event) => handleSelectAll(event.target.checked)}
                      aria-label="全选本页"
                    />
                  </th>
                )}
                {headers.map(renderHeaderCell)}
              </tr>
            </thead>
            <tbody>
              {pageMeta.length === 0 ? (
                <tr>
                  <td
                    colSpan={totalColumnCount}
                    {...stylex.props(styles.td, cellPadClass, styles.emptyCell)}
                  >
                    {emptyNode}
                  </td>
                </tr>
              ) : (
                pageMeta.map(({ row, key, disabled }) => {
                  const record = row.original;
                  const rowEvents = onRow ? onRow(record, row.index) : undefined;
                  const extraClass =
                    typeof rowClassName === 'function'
                      ? rowClassName(record, row.index)
                      : rowClassName;
                  const stylexRow = stylex.props(styles.row, onRow !== undefined && styles.rowClickable);
                  const className =
                    [stylexRow.className, extraClass].filter(Boolean).join(' ') || undefined;
                  return (
                    <tr
                      key={key}
                      {...rowEvents}
                      className={className}
                    >
                      {rowSelection && (
                        <td
                          {...stylex.props(styles.td, cellPadClass, styles.alignCenter)}
                        >
                          <input
                            type="checkbox"
                            {...stylex.props(styles.checkbox)}
                            checked={selectedSet.has(key)}
                            disabled={disabled}
                            onChange={() => handleRowToggle(key)}
                            aria-label="选择该行"
                          />
                        </td>
                      )}
                      {row.getAllCells().map((_cell, cellIndex) =>
                        renderBodyCell(row, columns[cellIndex], cellIndex),
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {isLoading && (
          <div {...stylex.props(styles.loadingMask)} role="status" aria-live="polite">
            <div {...stylex.props(styles.spinner)} aria-hidden="true" />
            <span {...stylex.props(styles.loadingTip)}>加载中…</span>
          </div>
        )}
      </div>

      {footer !== undefined && (
        <div {...stylex.props(styles.footer)}>
          {typeof footer === 'function' ? footer(dataSource) : footer}
        </div>
      )}

      {pag && (
        <div {...stylex.props(styles.pagerWrap)}>
          <Pagination
            current={current}
            pageSize={pageSize}
            total={total}
            onChange={handlePageChange}
            showSizeChanger={pag.showSizeChanger ?? total > 50}
            pageSizeOptions={pag.pageSizeOptions}
            showQuickJumper={pag.showQuickJumper}
            showTotal={pag.showTotal}
            disabled={pag.disabled}
          />
        </div>
      )}
    </div>
  );
}
