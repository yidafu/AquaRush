import { useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export type AvatarShape = 'circle' | 'square';
export type AvatarSize = number | 'small' | 'default' | 'large';

export interface AvatarProps {
  src?: string;
  alt?: string;
  size?: AvatarSize;
  shape?: AvatarShape;
  /** 无 src/加载失败时展示的图标 */
  icon?: ReactNode;
  /** 字母兜底取首字符用的名字(children 为字符串时优先 children) */
  name?: string;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const styles = stylex.create({
  base: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    flexShrink: '0',
    lineHeight: '1',
    verticalAlign: 'middle',
    boxSizing: 'border-box',
    userSelect: 'none',
    // 字母兜底:品牌蓝 tint 圆底 + 品牌蓝首字
    backgroundColor: tokens.statusInfoBg,
    color: tokens.brand,
    fontSize: '16px',
  },
  sizeSmall: {
    width: '24px',
    height: '24px',
    fontSize: '12px',
  },
  sizeDefault: {
    width: '32px',
    height: '32px',
    fontSize: '16px',
  },
  sizeLarge: {
    width: '40px',
    height: '40px',
    fontSize: '20px',
  },
  circle: {
    borderRadius: '50%',
  },
  square: {
    borderRadius: tokens.rSm,
  },
  img: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    display: 'block',
    // 加载失败时隐藏,露出兜底内容
  },
  imgBroken: {
    display: 'none',
  },
  icon: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1em',
    color: 'inherit',
  },
});

function sizeClass(size: AvatarSize) {
  switch (size) {
    case 'small':
      return styles.sizeSmall;
    case 'large':
      return styles.sizeLarge;
    case 'default':
      return styles.sizeDefault;
    default:
      return undefined;
  }
}

function letterOf(children: ReactNode, name?: string): string | null {
  const source = typeof children === 'string' ? children : name;
  if (source == null || source === '') return null;
  const trimmed = source.trim();
  return trimmed.length > 0 ? trimmed.charAt(0) : null;
}

export const Avatar: FC<AvatarProps> = ({ src, alt, size = 'default', shape = 'circle', icon, name, style, className, children }) => {
  const [broken, setBroken] = useState(false);
  const sp = stylex.props(styles.base, sizeClass(size), shape === 'square' ? styles.square : styles.circle);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  // 动态:size 为数字时的宽高与字号(StyleX 仅收静态值)
  const sizeStyle: CSSProperties | undefined =
    typeof size === 'number'
      ? { width: `${size}px`, height: `${size}px`, fontSize: `${Math.max(Math.round(size / 2), 8)}px` }
      : undefined;
  const mergedStyle = { ...sp.style, ...sizeStyle, ...style };

  const letter = letterOf(children, name);
  const fallback =
    typeof children === 'string' || letter != null ? (
      letter ?? null
    ) : children != null ? (
      children
    ) : icon != null ? (
      <span {...stylex.props(styles.icon)}>{icon}</span>
    ) : null;

  return (
    <span className={mergedClassName} style={mergedStyle}>
      {src != null && !broken && (
        <img
          {...stylex.props(styles.img)}
          src={src}
          alt={alt ?? ''}
          onError={() => setBroken(true)}
        />
      )}
      {(src == null || broken) && fallback}
    </span>
  );
};

export default Avatar;
