import { useState } from 'react';
import type { CSSProperties, FC, KeyboardEvent, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import type { StyleXStyles } from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { ArrowLeftOutlined } from './icons';

export type PaginationShowTotal = boolean | ((total: number, range: [number, number]) => ReactNode);

export interface PaginationProps {
  current?: number;
  pageSize?: number;
  defaultPageSize?: number;
  total?: number;
  onChange?: (page: number, pageSize: number) => void;
  pageSizeOptions?: (string | number)[];
  showSizeChanger?: boolean;
  showQuickJumper?: boolean;
  /** (total, [from, to]) => 节点;true = 「共 N 条」;false/缺省不显示 */
  showTotal?: PaginationShowTotal;
  disabled?: boolean;
  /** 简洁模式:页码折叠为「当前/总页」 */
  simple?: boolean;
  size?: 'default' | 'small';
  /** 总页数 ≤1 时不渲染 */
  hideOnSinglePage?: boolean;
  style?: CSSProperties;
  className?: string;
}

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    columnGap: '8px',
    rowGap: '8px',
    boxSizing: 'border-box',
    fontSize: '14px',
    color: tokens.text,
    userSelect: 'none',
  },
  total: {
    color: tokens.textSub,
    fontSize: '14px',
    lineHeight: '20px',
    whiteSpace: 'nowrap',
  },
  pages: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    listStyle: 'none',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
  item: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxSizing: 'border-box',
    width: '32px',
    height: '32px',
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.text,
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
    borderRadius: tokens.rSm,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    transitionProperty: 'border-color, color',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    ':hover': {
      borderColor: tokens.brand,
      color: tokens.brand,
    },
    ':focus-visible': {
      outlineStyle: 'solid',
      outlineWidth: '2px',
      outlineColor: tokens.brand,
      outlineOffset: '1px',
    },
  },
  itemSmall: {
    width: '24px',
    height: '24px',
    fontSize: '12px',
  },
  itemActive: {
    fontWeight: '500',
    backgroundColor: tokens.brand,
    borderColor: tokens.brand,
    color: tokens.onBrand,
    ':hover': {
      backgroundColor: tokens.brandDark,
      borderColor: tokens.brandDark,
      color: tokens.onBrand,
    },
  },
  itemDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      borderColor: tokens.border,
      color: tokens.textWeak,
    },
  },
  itemEllipsis: {
    cursor: 'default',
    color: tokens.textSub,
    ':hover': {
      borderColor: tokens.border,
      color: tokens.textSub,
    },
  },
  navIcon: {
    fontSize: '14px',
  },
  navIconNext: {
    transform: 'rotate(180deg)',
  },
  simpleText: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: '32px',
    height: '32px',
    fontSize: '14px',
    color: tokens.text,
    boxSizing: 'border-box',
  },
  sizeChanger: {
    display: 'inline-flex',
    alignItems: 'center',
  },
  sizeSelect: {
    height: '32px',
    boxSizing: 'border-box',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '8px',
    paddingRight: '8px',
    fontSize: '14px',
    color: tokens.text,
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderRadius: tokens.rSm,
    cursor: 'pointer',
    outlineStyle: 'none',
    ':focus-visible': {
      outlineStyle: 'solid',
      outlineWidth: '2px',
      outlineColor: tokens.brand,
      outlineOffset: '1px',
    },
  },
  sizeSelectSmall: {
    height: '24px',
    fontSize: '12px',
  },
  jumper: {
    display: 'inline-flex',
    alignItems: 'center',
    columnGap: '4px',
    rowGap: '0px',
    color: tokens.textSub,
    whiteSpace: 'nowrap',
  },
  jumperInput: {
    width: '48px',
    height: '32px',
    boxSizing: 'border-box',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '8px',
    paddingRight: '8px',
    fontSize: '14px',
    color: tokens.text,
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderRadius: tokens.rSm,
    textAlign: 'center',
    outlineStyle: 'none',
    ':focus-visible': {
      outlineStyle: 'solid',
      outlineWidth: '2px',
      outlineColor: tokens.brand,
      outlineOffset: '1px',
    },
  },
  jumperInputSmall: {
    height: '24px',
    fontSize: '12px',
  },
});

type PageToken = number | 'gap';

/** 页码序列(当前页 ±1 + 边缘窗口,缺口插省略号) */
function buildPages(current: number, pageCount: number): PageToken[] {
  const set = new Set<number>([1, pageCount]);
  for (let p = current - 1; p <= current + 1; p += 1) {
    if (p >= 1 && p <= pageCount) set.add(p);
  }
  if (current <= 4) {
    for (let p = 2; p <= 5; p += 1) if (p <= pageCount) set.add(p);
  }
  if (current >= pageCount - 3) {
    for (let p = pageCount - 4; p >= 2; p -= 1) if (p >= 2) set.add(p);
  }
  const sorted = [...set].filter((p) => p >= 1 && p <= pageCount).sort((a, b) => a - b);
  const tokensOut: PageToken[] = [];
  let prev = 0;
  for (const p of sorted) {
    if (prev !== 0 && p - prev > 1) tokensOut.push('gap');
    tokensOut.push(p);
    prev = p;
  }
  return tokensOut;
}

export const Pagination: FC<PaginationProps> = ({
  current = 1,
  pageSize,
  defaultPageSize = 10,
  total = 0,
  onChange,
  pageSizeOptions,
  showSizeChanger = false,
  showQuickJumper = false,
  showTotal,
  disabled = false,
  simple = false,
  size = 'default',
  hideOnSinglePage = false,
  style,
  className,
}) => {
  const [jumpValue, setJumpValue] = useState('');
  const resolvedPageSize = pageSize ?? defaultPageSize;
  const pageCount = Math.max(1, Math.ceil(total / resolvedPageSize));
  const page = Math.min(Math.max(current, 1), pageCount);
  const small = size === 'small';

  if (hideOnSinglePage && pageCount <= 1) return null;

  const from = total === 0 ? 0 : (page - 1) * resolvedPageSize + 1;
  const to = Math.min(page * resolvedPageSize, total);
  const options = (pageSizeOptions ?? ['10', '20', '50', '100']).map(String);

  const go = (nextPage: number, nextSize: number = resolvedPageSize) => {
    if (disabled) return;
    const clamped = Math.min(Math.max(nextPage, 1), Math.max(1, Math.ceil(total / nextSize)));
    if (clamped === page && nextSize === resolvedPageSize) return;
    onChange?.(clamped, nextSize);
  };

  const itemClass = (...extra: StyleXStyles[]) =>
    stylex.props(styles.item, small && styles.itemSmall, ...extra);

  const renderNavBtn = (dir: 'prev' | 'next') => (
    <button
      key={dir}
      type="button"
      aria-label={dir === 'prev' ? '上一页' : '下一页'}
      disabled={disabled || (dir === 'prev' ? page <= 1 : page >= pageCount)}
      {...itemClass(
        (disabled || (dir === 'prev' ? page <= 1 : page >= pageCount)) && styles.itemDisabled,
      )}
      onClick={() => go(dir === 'prev' ? page - 1 : page + 1)}
    >
      <ArrowLeftOutlined
        {...stylex.props(styles.navIcon, dir === 'next' && styles.navIconNext)}
      />
    </button>
  );

  const totalNode =
    showTotal != null && showTotal !== false && total > 0 ? (
      <span {...stylex.props(styles.total)}>
        {typeof showTotal === 'function' ? showTotal(total, [from, to]) : `共 ${total} 条`}
      </span>
    ) : null;

  const sizeChangerNode = showSizeChanger ? (
    <span {...stylex.props(styles.sizeChanger)}>
      <select
        {...stylex.props(styles.sizeSelect, small && styles.sizeSelectSmall)}
        value={resolvedPageSize}
        disabled={disabled}
        aria-label="每页条数"
        onChange={(e) => go(1, Number(e.target.value))}
      >
        {options.map((opt) => (
          <option key={opt} value={opt}>
            {opt} 条/页
          </option>
        ))}
      </select>
    </span>
  ) : null;

  const jumpTo = () => {
    const parsed = Number.parseInt(jumpValue, 10);
    if (Number.isNaN(parsed)) return;
    go(parsed);
    setJumpValue('');
  };

  const jumperNode = showQuickJumper ? (
    <span {...stylex.props(styles.jumper)}>
      跳至
      <input
        {...stylex.props(styles.jumperInput, small && styles.jumperInputSmall)}
        value={jumpValue}
        disabled={disabled}
        inputMode="numeric"
        aria-label="跳转页码"
        onChange={(e) => setJumpValue(e.target.value)}
        onBlur={jumpTo}
        onKeyDown={(e: KeyboardEvent<HTMLInputElement>) => {
          if (e.key === 'Enter') jumpTo();
        }}
      />
      页
    </span>
  ) : null;

  const pagesNode = simple ? (
    <span {...stylex.props(styles.pages)}>
      {renderNavBtn('prev')}
      <span {...stylex.props(styles.simpleText)}>
        {page}/{pageCount}
      </span>
      {renderNavBtn('next')}
    </span>
  ) : (
    <span {...stylex.props(styles.pages)}>
      {renderNavBtn('prev')}
      {buildPages(page, pageCount).map((token, index) =>
        token === 'gap' ? (
          <span key={`gap-${index}`} {...itemClass(styles.itemEllipsis)} aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={token}
            type="button"
            disabled={disabled}
            aria-current={token === page ? 'page' : undefined}
            {...itemClass(token === page && styles.itemActive, disabled && styles.itemDisabled)}
            onClick={() => go(token)}
          >
            {token}
          </button>
        ),
      )}
      {renderNavBtn('next')}
    </span>
  );

  const sp = stylex.props(styles.root);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  return (
    <div className={mergedClassName} style={mergedStyle}>
      {totalNode}
      {pagesNode}
      {sizeChangerNode}
      {jumperNode}
    </div>
  );
};

export default Pagination;
