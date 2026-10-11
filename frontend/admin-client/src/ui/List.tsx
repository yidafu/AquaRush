import type { CSSProperties, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { Spin } from './Spin';

export type ListSize = 'small' | 'default';

export interface ListProps<T = unknown> {
  dataSource?: T[];
  renderItem?: (item: T, index: number) => ReactNode;
  header?: ReactNode;
  footer?: ReactNode;
  bordered?: boolean;
  loading?: boolean;
  size?: ListSize;
  /** 行分隔线,缺省 true */
  split?: boolean;
  style?: CSSProperties;
  className?: string;
  /** 与 dataSource/renderItem 二选一的直接子节点 */
  children?: ReactNode;
}

const styles = stylex.create({
  root: {
    boxSizing: 'border-box',
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    color: tokens.text,
    backgroundColor: tokens.card,
    position: 'relative',
  },
  bordered: {
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
    borderRadius: tokens.rSm,
    overflow: 'hidden',
  },
  header: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: tokens.border,
    fontSize: '14px',
    fontWeight: '500',
    color: tokens.text,
    boxSizing: 'border-box',
  },
  footer: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderTopStyle: 'solid',
    borderTopWidth: '1px',
    borderTopColor: tokens.border,
    fontSize: '14px',
    color: tokens.textSub,
    boxSizing: 'border-box',
  },
  item: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
    boxSizing: 'border-box',
  },
  itemSmall: {
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '12px',
    paddingRight: '12px',
  },
  itemDivided: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: tokens.border,
  },
});

export function List<T>({
  dataSource,
  renderItem,
  header,
  footer,
  bordered = false,
  loading = false,
  size = 'default',
  split = true,
  style,
  className,
  children,
}: ListProps<T>) {
  const small = size === 'small';
  const sp = stylex.props(styles.root, bordered && styles.bordered);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  const entries: ReactNode[] =
    children != null
      ? Array.isArray(children)
        ? children
        : [children]
      : (dataSource ?? []).map((item, index) => (renderItem ? renderItem(item, index) : null));

  const total = entries.length;
  const body = entries.map((entry, index) => (
    <div
      key={index}
      {...stylex.props(styles.item, small && styles.itemSmall, split && index < total - 1 && styles.itemDivided)}
    >
      {entry}
    </div>
  ));

  return (
    <div className={mergedClassName} style={mergedStyle}>
      {header != null && <div {...stylex.props(styles.header)}>{header}</div>}
      {loading ? <Spin spinning>{body}</Spin> : body}
      {footer != null && <div {...stylex.props(styles.footer)}>{footer}</div>}
    </div>
  );
}

export default List;
