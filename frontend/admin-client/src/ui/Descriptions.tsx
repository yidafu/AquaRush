import { Children, Fragment, createContext, isValidElement, useContext, useEffect, useMemo, useState } from 'react';
import type { CSSProperties, FC, ReactElement, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface DescriptionsItemProps {
  key?: React.Key;
  label?: ReactNode;
  children?: ReactNode;
  /** 占用列数,缺省 1 */
  span?: number;
  style?: CSSProperties;
}

export type DescriptionsItem = DescriptionsItemProps;

export type DescriptionsColumn = number | { xs?: number; sm?: number; md?: number; lg?: number };

export interface DescriptionsProps {
  title?: ReactNode;
  extra?: ReactNode;
  /** 列数或响应式列数(antd 断点 576/768/992),缺省响应式 {xs:1, sm:2, md:3, lg:3} */
  column?: DescriptionsColumn;
  bordered?: boolean;
  size?: 'small' | 'default' | 'middle';
  /** label 后冒号(非 bordered 生效),缺省 true */
  colon?: boolean;
  /** v5+ 数据形态;与 children(Descriptions.Item)二选一 */
  items?: DescriptionsItem[];
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

interface DescriptionsContextValue {
  bordered: boolean;
  colon: boolean;
}

const DescriptionsContext = createContext<DescriptionsContextValue>({ bordered: false, colon: true });

// —— 响应式列数(互斥区间,单一 matchMedia 匹配)——
type ScreenKey = 'xs' | 'sm' | 'md' | 'lg';

const SCREEN_QUERIES: ReadonlyArray<readonly [ScreenKey, string]> = [
  ['xs', '(max-width: 575.98px)'],
  ['sm', '(min-width: 576px) and (max-width: 767.98px)'],
  ['md', '(min-width: 768px) and (max-width: 991.98px)'],
  ['lg', '(min-width: 992px)'],
];

/** antd DEFAULT_COLUMN_MAP 对齐 */
const DEFAULT_COLUMN: Record<ScreenKey, number> = { xs: 1, sm: 2, md: 3, lg: 3 };

function currentScreen(): ScreenKey {
  for (const [key, query] of SCREEN_QUERIES) {
    if (window.matchMedia(query).matches) return key;
  }
  return 'lg';
}

function useScreen(): ScreenKey {
  const [screen, setScreen] = useState<ScreenKey>(() => (typeof window === 'undefined' ? 'lg' : currentScreen()));
  useEffect(() => {
    const mqls = SCREEN_QUERIES.map(([, query]) => window.matchMedia(query));
    const handler = () => setScreen(currentScreen());
    handler();
    for (const mq of mqls) mq.addEventListener('change', handler);
    return () => {
      for (const mq of mqls) mq.removeEventListener('change', handler);
    };
  }, []);
  return screen;
}

function resolveColumn(column: DescriptionsColumn | undefined, screen: ScreenKey): number {
  if (typeof column === 'number') return column;
  if (column == null) return DEFAULT_COLUMN[screen];
  return column[screen] ?? DEFAULT_COLUMN[screen];
}

/** 按 column 贪心分行(antd useRow getCalcRows 同义,含末行 span 补齐) */
function calcRows(items: DescriptionsItem[], column: number): DescriptionsItem[][] {
  const rows: DescriptionsItem[][] = [];
  let tmp: DescriptionsItem[] = [];
  let count = 0;
  for (const item of items) {
    const span = item.span ?? 1;
    const restSpan = column - count;
    count += span;
    if (count >= column) {
      tmp.push(count > column ? { ...item, span: Math.max(restSpan, 1) } : item);
      rows.push(tmp);
      tmp = [];
      count = 0;
    } else {
      tmp.push(item);
    }
  }
  if (tmp.length > 0) rows.push(tmp);
  for (const row of rows) {
    const total = row.reduce((acc, it) => acc + (it.span ?? 1), 0);
    if (total < column && row.length > 0) {
      const last = row[row.length - 1];
      row[row.length - 1] = { ...last, span: column - (total - (last.span ?? 1)) };
    }
  }
  return rows;
}

const styles = stylex.create({
  root: {
    boxSizing: 'border-box',
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.text,
    width: '100%',
  },
  header: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '12px',
    rowGap: '0px',
    marginBottom: '16px',
  },
  title: {
    fontSize: '16px',
    lineHeight: '24px',
    fontWeight: '600',
    color: tokens.text,
    minWidth: '0px',
  },
  extra: {
    fontSize: '14px',
    color: tokens.textSub,
    flexShrink: '0',
  },
  view: {
    width: '100%',
    boxSizing: 'border-box',
  },
  viewBordered: {
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
    borderRadius: tokens.rSm,
    overflow: 'hidden',
  },
  table: {
    width: '100%',
    tableLayout: 'auto',
    borderCollapse: 'collapse',
    boxSizing: 'border-box',
  },
  row: {
    // 行/单元格边框与内边距按状态类组合,避免同属性跨类竞争
  },
  // —— 非 bordered:td ——
  cellPlain: {
    paddingTop: '0px',
    paddingBottom: '16px',
    paddingLeft: '0px',
    paddingRight: '16px',
    verticalAlign: 'top',
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.text,
    wordBreak: 'break-word',
    overflowWrap: 'break-word',
  },
  cellPlainMiddle: {
    paddingBottom: '12px',
  },
  cellPlainSmall: {
    paddingBottom: '8px',
  },
  cellPlainLastCol: {
    paddingRight: '0px',
  },
  // —— bordered:th/td ——
  cellBordered: {
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '24px',
    paddingRight: '24px',
    verticalAlign: 'top',
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.text,
    wordBreak: 'break-word',
    overflowWrap: 'break-word',
    borderLeftStyle: 'solid',
    borderLeftWidth: '1px',
    borderLeftColor: tokens.border,
  },
  cellBorderedMiddle: {
    paddingTop: '12px',
    paddingBottom: '12px',
  },
  cellBorderedSmall: {
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  cellBorderedFirstCol: {
    borderLeftWidth: '0px',
  },
  borderedRow: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: tokens.border,
  },
  borderedRowLast: {
    borderBottomWidth: '0px',
  },
  labelCellBordered: {
    // antd colorFillAlter 底(tokens 无对应,字面量保留)
    backgroundColor: '#FAFAFA',
    color: tokens.textSub,
    fontWeight: '400',
    textAlign: 'left',
  },
  contentCellBordered: {
    color: tokens.text,
  },
  // —— 非 bordered 容器 ——
  plainContainer: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'baseline',
    flexWrap: 'wrap',
    columnGap: '0px',
    rowGap: '0px',
    minWidth: '0px',
  },
  label: {
    display: 'inline-flex',
    alignItems: 'baseline',
    color: tokens.textSub,
    paddingRight: '0px',
    flexShrink: '0',
    maxWidth: '100%',
  },
  colon: {
    marginLeft: '2px',
    marginRight: '8px',
    color: tokens.textSub,
    userSelect: 'none',
  },
  content: {
    display: 'inline',
    color: tokens.text,
    minWidth: '1em',
    wordBreak: 'break-word',
    overflowWrap: 'break-word',
  },
});

type CellSize = 'small' | 'default' | 'middle';

function plainCellClass(size: CellSize, lastCol: boolean) {
  return [
    styles.cellPlain,
    size === 'middle' ? styles.cellPlainMiddle : size === 'small' ? styles.cellPlainSmall : undefined,
    lastCol && styles.cellPlainLastCol,
  ];
}

function borderedCellClass(size: CellSize, firstCol: boolean) {
  return [
    styles.cellBordered,
    size === 'middle' ? styles.cellBorderedMiddle : size === 'small' ? styles.cellBorderedSmall : undefined,
    firstCol && styles.cellBorderedFirstCol,
  ];
}

const DescriptionsRoot: FC<DescriptionsProps> = ({
  title,
  extra,
  column,
  bordered = false,
  size = 'default',
  colon = true,
  items,
  style,
  className,
  children,
}) => {
  const screen = useScreen();
  const mergedColumn = resolveColumn(column, screen);
  const mergedSize: CellSize = size === 'default' ? 'default' : size;

  const collected = useMemo<DescriptionsItem[]>(() => {
    const out: DescriptionsItem[] = [];
    const pushChild = (child: ReactNode) => {
      if (child == null || typeof child === 'boolean') return;
      if (Array.isArray(child)) {
        child.forEach(pushChild);
        return;
      }
      if (!isValidElement(child) || !isValidItemElement(child)) return;
      out.push(child.props);
    };
    if (items != null) {
      for (const item of items) out.push(item);
    } else {
      Children.forEach(children, pushChild);
    }
    return out;
  }, [items, children]);

  const rows = useMemo(() => calcRows(collected, mergedColumn), [collected, mergedColumn]);

  const sp = stylex.props(styles.root);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  const contextValue = useMemo(() => ({ bordered, colon }), [bordered, colon]);

  return (
    <DescriptionsContext.Provider value={contextValue}>
      <div className={mergedClassName} style={mergedStyle}>
        {(title != null || extra != null) && (
          <div {...stylex.props(styles.header)}>
            {title != null && <div {...stylex.props(styles.title)}>{title}</div>}
            {extra != null && <div {...stylex.props(styles.extra)}>{extra}</div>}
          </div>
        )}
        <div {...stylex.props(styles.view, bordered && styles.viewBordered)}>
          <table {...stylex.props(styles.table)}>
            <tbody>
              {rows.map((row, rowIndex) => (
                <tr
                  key={rowIndex}
                  {...stylex.props(
                    styles.row,
                    bordered && (rowIndex < rows.length - 1 ? styles.borderedRow : styles.borderedRowLast),
                  )}
                >
                  {row.map((item, colIndex) => (
                    <ItemCells
                      key={item.key ?? colIndex}
                      item={item}
                      size={mergedSize}
                      firstCol={colIndex === 0}
                      lastCol={colIndex === row.length - 1}
                    />
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </DescriptionsContext.Provider>
  );
};

interface ItemCellsProps {
  item: DescriptionsItem;
  size: CellSize;
  firstCol: boolean;
  lastCol: boolean;
}

const ItemCells: FC<ItemCellsProps> = ({ item, size, firstCol, lastCol }) => {
  const { bordered, colon } = useContext(DescriptionsContext);
  const span = item.span ?? 1;
  const labelNode = (
    <>
      {item.label}
      {colon && !bordered && <span {...stylex.props(styles.colon)}>:</span>}
    </>
  );

  if (bordered) {
    return (
      <>
        <th {...stylex.props(...borderedCellClass(size, firstCol))} style={item.style}>
          <span {...stylex.props(styles.label, styles.labelCellBordered)}>{labelNode}</span>
        </th>
        <td colSpan={span * 2 - 1} {...stylex.props(styles.contentCellBordered)} style={item.style}>
          <span {...stylex.props(styles.content)}>{item.children}</span>
        </td>
      </>
    );
  }

  return (
    <td colSpan={span} {...stylex.props(...plainCellClass(size, lastCol))} style={item.style}>
      <div {...stylex.props(styles.plainContainer)}>
        <span {...stylex.props(styles.label)}>{labelNode}</span>
        <span {...stylex.props(styles.content)}>{item.children}</span>
      </div>
    </td>
  );
};

function isValidItemElement(
  child: ReactElement,
): child is ReactElement<DescriptionsItemProps> {
  return child.type === Fragment || child.type === Item || 'label' in (child.props as object);
}

/** antd Descriptions.Item:仅作 JSX 标记糖,渲染由 Descriptions 收集 props 完成 */
const Item: FC<DescriptionsItemProps> = ({ children }) => <>{children}</>;

export type DescriptionsComponent = FC<DescriptionsProps> & {
  Item: FC<DescriptionsItemProps>;
};

export const Descriptions: DescriptionsComponent = Object.assign(DescriptionsRoot, { Item });

export default Descriptions;
