import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { Spin } from './Spin';

export type CardSize = 'default' | 'small';

export interface CardProps {
  title?: ReactNode;
  /** 右上角操作区 */
  extra?: ReactNode;
  /** 底部操作条 */
  actions?: ReactNode[];
  bordered?: boolean;
  size?: CardSize;
  loading?: boolean;
  /** 悬停加深阴影 */
  hoverable?: boolean;
  /** 顶部覆盖内容(图片等) */
  cover?: ReactNode;
  headStyle?: CSSProperties;
  bodyStyle?: CSSProperties;
  /** antd v5 语义化样式(header/body),与 headStyle/bodyStyle 合并 */
  styles?: { header?: CSSProperties; body?: CSSProperties };
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const styles = stylex.create({
  root: {
    backgroundColor: tokens.card,
    borderRadius: tokens.rCard,
    boxShadow: tokens.shCard,
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    color: tokens.text,
    boxSizing: 'border-box',
  },
  bordered: {
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
  },
  hoverable: {
    transitionProperty: 'box-shadow',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    ':hover': {
      // 悬停阴影(设计 token 无对应值,保留字面量)
      boxShadow: '0 8px 24px rgba(0, 0, 0, 0.08)',
    },
  },
  head: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '12px',
    rowGap: '0px',
    boxSizing: 'border-box',
    // 设计规范卡片 padding 16(spec/design/ui-design-system.md §4)
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: tokens.border,
  },
  headSmall: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  title: {
    fontSize: tokens.fsBlock,
    lineHeight: tokens.lhBlock,
    fontWeight: '600',
    color: tokens.text,
    minWidth: '0px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  extra: {
    marginLeft: 'auto',
    flexShrink: '0',
    display: 'flex',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '0px',
  },
  body: {
    // 设计规范卡片 padding 16(spec/design/ui-design-system.md §4)
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
    paddingRight: '16px',
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    boxSizing: 'border-box',
    // padding 由 bodyStyle/styles.body 决定,保留 style={} 动态注入
  },
  bodySmall: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  cover: {
    borderTopLeftRadius: tokens.rCard,
    borderTopRightRadius: tokens.rCard,
    overflow: 'hidden',
    boxSizing: 'border-box',
  },
  actions: {
    display: 'flex',
    flexDirection: 'row',
    listStyle: 'none',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderTopStyle: 'solid',
    borderTopWidth: '1px',
    borderTopColor: tokens.border,
    borderRadius: '0px',
    backgroundColor: 'transparent',
  },
  actionItem: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '12px',
    paddingBottom: '12px',
    fontSize: '14px',
    color: tokens.textSub,
    textAlign: 'center',
    minWidth: '0px',
  },
  actionItemDivided: {
    borderLeftStyle: 'solid',
    borderLeftWidth: '1px',
    borderLeftColor: tokens.border,
  },
});

export const Card: FC<CardProps> = ({
  title,
  extra,
  actions,
  bordered = true,
  size = 'default',
  loading = false,
  hoverable = false,
  cover,
  headStyle,
  bodyStyle,
  styles: semanticStyles,
  style,
  className,
  children,
}) => {
  const small = size === 'small';
  const showHead = (title != null && title !== false) || extra != null;
  const sp = stylex.props(styles.root, bordered && styles.bordered, hoverable && styles.hoverable);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  // 动态:headStyle/styles.body 等 props 决定的值留在 style={}
  const headStyleMerged: CSSProperties = { ...semanticStyles?.header, ...headStyle };
  const bodyStyleMerged: CSSProperties = { ...semanticStyles?.body, ...bodyStyle };

  return (
    <div className={mergedClassName} style={mergedStyle}>
      {cover != null && <div {...stylex.props(styles.cover)}>{cover}</div>}
      {showHead && (
        <div
          {...stylex.props(styles.head, small && styles.headSmall)}
          style={headStyleMerged}
        >
          {title != null && title !== false && <div {...stylex.props(styles.title)}>{title}</div>}
          {extra != null && <div {...stylex.props(styles.extra)}>{extra}</div>}
        </div>
      )}
      <div
        {...stylex.props(styles.body, small && styles.bodySmall)}
        style={bodyStyleMerged}
      >
        {loading ? <Spin spinning>{children}</Spin> : children}
      </div>
      {actions != null && actions.length > 0 && (
        <ul {...stylex.props(styles.actions)}>
          {actions.map((action, index) => (
            <li
              key={index}
              {...stylex.props(styles.actionItem, index > 0 && styles.actionItemDivided)}
            >
              {action}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default Card;
