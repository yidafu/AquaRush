import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface BreadcrumbItem {
  title: ReactNode;
  /** 有 href 时渲染为链接(最后一项始终为纯文本) */
  href?: string;
}

export interface BreadcrumbProps {
  items?: BreadcrumbItem[];
  /** 分隔符,缺省 '/' */
  separator?: ReactNode;
  style?: CSSProperties;
  className?: string;
  /** items 之外的 children 形态(节点间插分隔符) */
  children?: ReactNode;
}

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.textSub,
    boxSizing: 'border-box',
  },
  item: {
    color: tokens.textSub,
    minWidth: '0px',
  },
  link: {
    color: tokens.textSub,
    textDecoration: 'none',
    cursor: 'pointer',
    ':hover': {
      color: tokens.brand,
    },
  },
  current: {
    color: tokens.text,
    fontWeight: '400',
  },
  separator: {
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '8px',
    marginRight: '8px',
    color: tokens.textWeak,
    userSelect: 'none',
    flexShrink: '0',
  },
  contents: {
    display: 'contents',
  },
});

export const Breadcrumb: FC<BreadcrumbProps> = ({ items, separator = '/', style, className, children }) => {
  const sp = stylex.props(styles.root);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  const nodes: ReactNode[] =
    items != null
      ? items.map((item, index) => {
          const isLast = index === items.length - 1;
          const node =
            item.href != null && !isLast ? (
              <a {...stylex.props(styles.item, styles.link)} href={item.href}>
                {item.title}
              </a>
            ) : (
              <span {...stylex.props(styles.item, isLast && styles.current)}>{item.title}</span>
            );
          return (
            <span key={index} {...stylex.props(styles.contents)}>
              {index > 0 && (
                <span {...stylex.props(styles.separator)} aria-hidden="true">
                  {separator}
                </span>
              )}
              {node}
            </span>
          );
        })
      : // children 形态:节点间插分隔符
        [];

  const childNodes = children != null && items == null
    ? Array.isArray(children)
      ? children
      : [children]
    : [];

  return (
    <nav className={mergedClassName} style={mergedStyle}>
      {nodes}
      {childNodes.map((child, index) => (
        <span key={index} {...stylex.props(styles.contents)}>
          {index > 0 && (
            <span {...stylex.props(styles.separator)} aria-hidden="true">
              {separator}
            </span>
          )}
          {child}
        </span>
      ))}
    </nav>
  );
};

export default Breadcrumb;
