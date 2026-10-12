import { createContext, useContext } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import type { StyleXStyles } from '@stylexjs/stylex';

export interface RowProps {
  /** 栅格间距:[水平, 垂直] 或单值(水平=垂直) */
  gutter?: number | [number, number];
  /** 垂直对齐 */
  align?: 'top' | 'middle' | 'bottom';
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

export interface ColProps {
  /** 列宽(24 栅格) */
  span?: number;
  /** 响应式断点宽度(antd 断点 576/768/992) */
  xs?: number;
  sm?: number;
  md?: number;
  lg?: number;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

/** Row 的 gutter 经 context 下发给 Col(负边距 + 对称内边距模式) */
const GutterContext = createContext<{ h: number; v: number }>({ h: 0, v: 0 });

function parseGutter(gutter?: number | [number, number]): { h: number; v: number } {
  if (gutter == null) return { h: 0, v: 0 };
  if (typeof gutter === 'number') return { h: gutter, v: gutter };
  return { h: gutter[0] ?? 0, v: gutter[1] ?? 0 };
}

const styles = stylex.create({
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    boxSizing: 'border-box',
    // 负边距 gutter 由 props 决定,保留 style={} 动态注入
  },
  alignTop: {
    alignItems: 'flex-start',
  },
  alignMiddle: {
    alignItems: 'center',
  },
  alignBottom: {
    alignItems: 'flex-end',
  },
  col: {
    boxSizing: 'border-box',
    position: 'relative',
    maxWidth: '100%',
    // 对称内边距 gutter 由 context 决定,保留 style={} 动态注入
  },
  span1: { width: '4.166667%' },
  span2: { width: '8.333333%' },
  span3: { width: '12.5%' },
  span4: { width: '16.666667%' },
  span5: { width: '20.833333%' },
  span6: { width: '25%' },
  span7: { width: '29.166667%' },
  span8: { width: '33.333333%' },
  span9: { width: '37.5%' },
  span10: { width: '41.666667%' },
  span11: { width: '45.833333%' },
  span12: { width: '50%' },
  span13: { width: '54.166667%' },
  span14: { width: '58.333333%' },
  span15: { width: '62.5%' },
  span16: { width: '66.666667%' },
  span17: { width: '70.833333%' },
  span18: { width: '75%' },
  span19: { width: '79.166667%' },
  span20: { width: '83.333333%' },
  span21: { width: '87.5%' },
  span22: { width: '91.666667%' },
  span23: { width: '95.833333%' },
  span24: { width: '100%' },
  xs1: { '@media (max-width: 575.98px)': { width: '4.166667%' } },
  xs2: { '@media (max-width: 575.98px)': { width: '8.333333%' } },
  xs3: { '@media (max-width: 575.98px)': { width: '12.5%' } },
  xs4: { '@media (max-width: 575.98px)': { width: '16.666667%' } },
  xs5: { '@media (max-width: 575.98px)': { width: '20.833333%' } },
  xs6: { '@media (max-width: 575.98px)': { width: '25%' } },
  xs7: { '@media (max-width: 575.98px)': { width: '29.166667%' } },
  xs8: { '@media (max-width: 575.98px)': { width: '33.333333%' } },
  xs9: { '@media (max-width: 575.98px)': { width: '37.5%' } },
  xs10: { '@media (max-width: 575.98px)': { width: '41.666667%' } },
  xs11: { '@media (max-width: 575.98px)': { width: '45.833333%' } },
  xs12: { '@media (max-width: 575.98px)': { width: '50%' } },
  xs13: { '@media (max-width: 575.98px)': { width: '54.166667%' } },
  xs14: { '@media (max-width: 575.98px)': { width: '58.333333%' } },
  xs15: { '@media (max-width: 575.98px)': { width: '62.5%' } },
  xs16: { '@media (max-width: 575.98px)': { width: '66.666667%' } },
  xs17: { '@media (max-width: 575.98px)': { width: '70.833333%' } },
  xs18: { '@media (max-width: 575.98px)': { width: '75%' } },
  xs19: { '@media (max-width: 575.98px)': { width: '79.166667%' } },
  xs20: { '@media (max-width: 575.98px)': { width: '83.333333%' } },
  xs21: { '@media (max-width: 575.98px)': { width: '87.5%' } },
  xs22: { '@media (max-width: 575.98px)': { width: '91.666667%' } },
  xs23: { '@media (max-width: 575.98px)': { width: '95.833333%' } },
  xs24: { '@media (max-width: 575.98px)': { width: '100%' } },
  sm1: { '@media (min-width: 576px)': { width: '4.166667%' } },
  sm2: { '@media (min-width: 576px)': { width: '8.333333%' } },
  sm3: { '@media (min-width: 576px)': { width: '12.5%' } },
  sm4: { '@media (min-width: 576px)': { width: '16.666667%' } },
  sm5: { '@media (min-width: 576px)': { width: '20.833333%' } },
  sm6: { '@media (min-width: 576px)': { width: '25%' } },
  sm7: { '@media (min-width: 576px)': { width: '29.166667%' } },
  sm8: { '@media (min-width: 576px)': { width: '33.333333%' } },
  sm9: { '@media (min-width: 576px)': { width: '37.5%' } },
  sm10: { '@media (min-width: 576px)': { width: '41.666667%' } },
  sm11: { '@media (min-width: 576px)': { width: '45.833333%' } },
  sm12: { '@media (min-width: 576px)': { width: '50%' } },
  sm13: { '@media (min-width: 576px)': { width: '54.166667%' } },
  sm14: { '@media (min-width: 576px)': { width: '58.333333%' } },
  sm15: { '@media (min-width: 576px)': { width: '62.5%' } },
  sm16: { '@media (min-width: 576px)': { width: '66.666667%' } },
  sm17: { '@media (min-width: 576px)': { width: '70.833333%' } },
  sm18: { '@media (min-width: 576px)': { width: '75%' } },
  sm19: { '@media (min-width: 576px)': { width: '79.166667%' } },
  sm20: { '@media (min-width: 576px)': { width: '83.333333%' } },
  sm21: { '@media (min-width: 576px)': { width: '87.5%' } },
  sm22: { '@media (min-width: 576px)': { width: '91.666667%' } },
  sm23: { '@media (min-width: 576px)': { width: '95.833333%' } },
  sm24: { '@media (min-width: 576px)': { width: '100%' } },
  md1: { '@media (min-width: 768px)': { width: '4.166667%' } },
  md2: { '@media (min-width: 768px)': { width: '8.333333%' } },
  md3: { '@media (min-width: 768px)': { width: '12.5%' } },
  md4: { '@media (min-width: 768px)': { width: '16.666667%' } },
  md5: { '@media (min-width: 768px)': { width: '20.833333%' } },
  md6: { '@media (min-width: 768px)': { width: '25%' } },
  md7: { '@media (min-width: 768px)': { width: '29.166667%' } },
  md8: { '@media (min-width: 768px)': { width: '33.333333%' } },
  md9: { '@media (min-width: 768px)': { width: '37.5%' } },
  md10: { '@media (min-width: 768px)': { width: '41.666667%' } },
  md11: { '@media (min-width: 768px)': { width: '45.833333%' } },
  md12: { '@media (min-width: 768px)': { width: '50%' } },
  md13: { '@media (min-width: 768px)': { width: '54.166667%' } },
  md14: { '@media (min-width: 768px)': { width: '58.333333%' } },
  md15: { '@media (min-width: 768px)': { width: '62.5%' } },
  md16: { '@media (min-width: 768px)': { width: '66.666667%' } },
  md17: { '@media (min-width: 768px)': { width: '70.833333%' } },
  md18: { '@media (min-width: 768px)': { width: '75%' } },
  md19: { '@media (min-width: 768px)': { width: '79.166667%' } },
  md20: { '@media (min-width: 768px)': { width: '83.333333%' } },
  md21: { '@media (min-width: 768px)': { width: '87.5%' } },
  md22: { '@media (min-width: 768px)': { width: '91.666667%' } },
  md23: { '@media (min-width: 768px)': { width: '95.833333%' } },
  md24: { '@media (min-width: 768px)': { width: '100%' } },
  lg1: { '@media (min-width: 992px)': { width: '4.166667%' } },
  lg2: { '@media (min-width: 992px)': { width: '8.333333%' } },
  lg3: { '@media (min-width: 992px)': { width: '12.5%' } },
  lg4: { '@media (min-width: 992px)': { width: '16.666667%' } },
  lg5: { '@media (min-width: 992px)': { width: '20.833333%' } },
  lg6: { '@media (min-width: 992px)': { width: '25%' } },
  lg7: { '@media (min-width: 992px)': { width: '29.166667%' } },
  lg8: { '@media (min-width: 992px)': { width: '33.333333%' } },
  lg9: { '@media (min-width: 992px)': { width: '37.5%' } },
  lg10: { '@media (min-width: 992px)': { width: '41.666667%' } },
  lg11: { '@media (min-width: 992px)': { width: '45.833333%' } },
  lg12: { '@media (min-width: 992px)': { width: '50%' } },
  lg13: { '@media (min-width: 992px)': { width: '54.166667%' } },
  lg14: { '@media (min-width: 992px)': { width: '58.333333%' } },
  lg15: { '@media (min-width: 992px)': { width: '62.5%' } },
  lg16: { '@media (min-width: 992px)': { width: '66.666667%' } },
  lg17: { '@media (min-width: 992px)': { width: '70.833333%' } },
  lg18: { '@media (min-width: 992px)': { width: '75%' } },
  lg19: { '@media (min-width: 992px)': { width: '79.166667%' } },
  lg20: { '@media (min-width: 992px)': { width: '83.333333%' } },
  lg21: { '@media (min-width: 992px)': { width: '87.5%' } },
  lg22: { '@media (min-width: 992px)': { width: '91.666667%' } },
  lg23: { '@media (min-width: 992px)': { width: '95.833333%' } },
  lg24: { '@media (min-width: 992px)': { width: '100%' } },
});

function alignStyle(align?: 'top' | 'middle' | 'bottom') {
  switch (align) {
    case 'top':
      return styles.alignTop;
    case 'middle':
      return styles.alignMiddle;
    case 'bottom':
      return styles.alignBottom;
    default:
      return undefined;
  }
}

/** 栅格行(antd Row 同形):gutter 负边距 + Col 对称内边距 */
export const Row: FC<RowProps> = ({ gutter, align, style, className, children }) => {
  const g = parseGutter(gutter);
  const sp = stylex.props(styles.row, alignStyle(align));
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  // 动态 gutter → 行内负边距(StyleX 仅收静态值)
  const rowStyle: CSSProperties = {
    marginLeft: g.h !== 0 ? `${-g.h / 2}px` : undefined,
    marginRight: g.h !== 0 ? `${-g.h / 2}px` : undefined,
    marginTop: g.v !== 0 ? `${-g.v / 2}px` : undefined,
    marginBottom: g.v !== 0 ? `${-g.v / 2}px` : undefined,
    ...sp.style,
    ...style,
  };
  return (
    <GutterContext.Provider value={g}>
      <div className={mergedClassName} style={rowStyle}>
        {children}
      </div>
    </GutterContext.Provider>
  );
};

/** span1..24 / xs|sm|md|lg1..24 静态宽度类的键值查表(超出 1..24 返回 undefined) */
const WIDTH_TABLE = styles as unknown as Record<string, StyleXStyles | undefined>;

function widthStyle(bp: 'xs' | 'sm' | 'md' | 'lg' | 'span', value?: number) {
  if (value == null || value < 1 || value > 24) return undefined;
  return WIDTH_TABLE[`${bp}${value}`];
}

/** 栅格列(antd Col 同形):span + xs/sm/md/lg 响应式宽度 */
export const Col: FC<ColProps> = ({ span, xs, sm, md, lg, style, className, children }) => {
  const gutter = useContext(GutterContext);
  const sp = stylex.props(
    styles.col,
    widthStyle('span', span),
    widthStyle('xs', xs),
    widthStyle('sm', sm),
    widthStyle('md', md),
    widthStyle('lg', lg),
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  // 动态 gutter 内边距(经 Row context)→ 行内样式
  const colStyle: CSSProperties = {
    paddingLeft: gutter.h !== 0 ? `${gutter.h / 2}px` : undefined,
    paddingRight: gutter.h !== 0 ? `${gutter.h / 2}px` : undefined,
    paddingTop: gutter.v !== 0 ? `${gutter.v / 2}px` : undefined,
    paddingBottom: gutter.v !== 0 ? `${gutter.v / 2}px` : undefined,
    ...sp.style,
    ...style,
  };
  return (
    <div className={mergedClassName} style={colStyle}>
      {children}
    </div>
  );
};
