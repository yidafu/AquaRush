import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface DividerProps {
  /** 文字位置,缺省 center */
  orientation?: 'left' | 'center' | 'right';
  /** 弱化文字(常规字重、小字号,antd plain 同义) */
  plain?: boolean;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const styles = stylex.create({
  // 纯分隔线(antd horizontal 无文字:margin 24px 0)
  line: {
    display: 'block',
    width: '100%',
    height: '1px',
    boxSizing: 'border-box',
    backgroundColor: tokens.border,
    borderStyle: 'none',
    borderWidth: '0px',
    marginTop: '24px',
    marginBottom: '24px',
    marginLeft: '0px',
    marginRight: '0px',
  },
  // 带文字分隔线(antd with-text:margin 16px 0)
  withText: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
    boxSizing: 'border-box',
    marginTop: '16px',
    marginBottom: '16px',
    marginLeft: '0px',
    marginRight: '0px',
    fontSize: '16px',
    fontWeight: '500',
    lineHeight: '24px',
    color: tokens.text,
    whiteSpace: 'nowrap',
  },
  plain: {
    fontSize: '14px',
    fontWeight: '400',
    lineHeight: '20px',
  },
  center: {
    justifyContent: 'center',
  },
  left: {
    justifyContent: 'flex-start',
  },
  right: {
    justifyContent: 'flex-end',
  },
  rail: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    height: '1px',
    boxSizing: 'border-box',
    backgroundColor: tokens.border,
    minWidth: '0px',
  },
  text: {
    paddingLeft: '16px',
    paddingRight: '16px',
    fontSize: 'inherit',
    fontWeight: 'inherit',
    color: 'inherit',
  },
});

export const Divider: FC<DividerProps> = ({ orientation = 'center', plain = false, style, className, children }) => {
  const hasText = children != null && children !== false && children !== '';
  const sp = hasText
    ? stylex.props(
        styles.withText,
        plain && styles.plain,
        orientation === 'left' ? styles.left : orientation === 'right' ? styles.right : styles.center,
      )
    : stylex.props(styles.line);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  if (!hasText) {
    return <div className={mergedClassName} style={mergedStyle} role="separator" />;
  }

  const textNode = <span {...stylex.props(styles.text)}>{children}</span>;
  return (
    <div className={mergedClassName} style={mergedStyle} role="separator">
      {orientation !== 'left' && <span {...stylex.props(styles.rail)} />}
      {textNode}
      {orientation !== 'right' && <span {...stylex.props(styles.rail)} />}
    </div>
  );
};

export default Divider;
