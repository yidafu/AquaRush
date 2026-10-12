import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';

export type SpaceSize = 'small' | 'middle' | 'large' | number;
export type SpaceAlign = 'start' | 'end' | 'center' | 'baseline';

export interface SpaceProps {
  direction?: 'horizontal' | 'vertical';
  /** antd 尺寸档:small=8 / middle=12 / large=16,或自定义像素 */
  size?: SpaceSize;
  align?: SpaceAlign;
  wrap?: boolean;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const styles = stylex.create({
  base: {
    display: 'inline-flex',
    boxSizing: 'border-box',
    // gap 数值由 size prop 决定,保留 style={} 动态注入(静态档位走 sizeXxx 类)
  },
  horizontal: {
    flexDirection: 'row',
  },
  vertical: {
    flexDirection: 'column',
  },
  wrap: {
    flexWrap: 'wrap',
  },
  sizeSmall: {
    columnGap: '8px',
    rowGap: '8px',
  },
  sizeMiddle: {
    columnGap: '12px',
    rowGap: '12px',
  },
  sizeLarge: {
    columnGap: '16px',
    rowGap: '16px',
  },
  alignStart: {
    alignItems: 'flex-start',
  },
  alignEnd: {
    alignItems: 'flex-end',
  },
  alignCenter: {
    alignItems: 'center',
  },
  alignBaseline: {
    alignItems: 'baseline',
  },
});

function sizeClass(size: SpaceSize) {
  switch (size) {
    case 'small':
      return styles.sizeSmall;
    case 'middle':
      return undefined;
    case 'large':
      return styles.sizeLarge;
    default:
      return undefined;
  }
}

function alignClass(align?: SpaceAlign) {
  switch (align) {
    case 'start':
      return styles.alignStart;
    case 'end':
      return styles.alignEnd;
    case 'center':
      return styles.alignCenter;
    case 'baseline':
      return styles.alignBaseline;
    default:
      return undefined;
  }
}

export const Space: FC<SpaceProps> = ({
  direction = 'horizontal',
  size = 'small',
  align,
  wrap = false,
  style,
  className,
  children,
}) => {
  const sp = stylex.props(
    styles.base,
    direction === 'vertical' ? styles.vertical : styles.horizontal,
    wrap && styles.wrap,
    sizeClass(size),
    alignClass(align),
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  // 动态:size 为数字时的间距(StyleX 仅收静态值)
  const sizeStyle: CSSProperties | undefined =
    typeof size === 'number' ? { columnGap: `${size}px`, rowGap: `${size}px` } : undefined;
  const mergedStyle = { ...sp.style, ...sizeStyle, ...style };
  return (
    <div className={mergedClassName} style={mergedStyle}>
      {children}
    </div>
  );
};

export default Space;
