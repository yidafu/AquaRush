import { useState } from 'react';
import type { CSSProperties, FC, MouseEvent, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { CloseOutlined } from './icons';

export interface TagProps {
  /** antd 语义色名(success/warning/error/processing/default)、预设色名或自定义色值 */
  color?: string;
  closable?: boolean;
  onClose?: (e: MouseEvent<HTMLElement>) => void;
  /** 文案前图标 */
  icon?: ReactNode;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const styles = stylex.create({
  base: {
    display: 'inline-flex',
    alignItems: 'center',
    boxSizing: 'border-box',
    // 设计规范状态标签:圆角 8、字号 12、内边距 4×8(spec/design/ui-design-system.md §2.2)
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    paddingRight: '8px',
    fontSize: '12px',
    lineHeight: '16px',
    borderRadius: tokens.rSm,
    whiteSpace: 'nowrap',
    maxWidth: '100%',
    verticalAlign: 'middle',
  },
  // —— 状态色对(tokens 10% tint 底 + 状态色文字,与 STATUS_TONE_COLORS 对齐)——
  default: {
    backgroundColor: tokens.statusNeutralBg,
    color: tokens.statusNeutralFg,
  },
  success: {
    backgroundColor: tokens.statusSuccessBg,
    color: tokens.statusSuccessFg,
  },
  warning: {
    backgroundColor: tokens.statusWarningBg,
    color: tokens.statusWarningFg,
  },
  error: {
    backgroundColor: tokens.statusErrorBg,
    color: tokens.statusErrorFg,
  },
  processing: {
    backgroundColor: tokens.statusInfoBg,
    color: tokens.statusInfoFg,
  },
  // —— antd 预设色(轻底色对 = antd palette color-1 底 / color-6 文字;tokens 无对应,字面量保留)——
  blue: {
    backgroundColor: '#E6F4FF',
    color: '#1677FF',
  },
  green: {
    backgroundColor: '#F6FFED',
    color: '#52C41A',
  },
  red: {
    backgroundColor: '#FFF1F0',
    color: '#FF4D4F',
  },
  orange: {
    backgroundColor: '#FFF7E6',
    color: '#FA8C16',
  },
  gold: {
    backgroundColor: '#FFFBE6',
    color: '#FAAD14',
  },
  purple: {
    backgroundColor: '#F9F0FF',
    color: '#722ED1',
  },
  magenta: {
    backgroundColor: '#FFF0F6',
    color: '#EB2F96',
  },
  geekblue: {
    backgroundColor: '#F0F5FF',
    color: '#2F54EB',
  },
  cyan: {
    backgroundColor: '#E6FFFB',
    color: '#13C2C2',
  },
  volcano: {
    backgroundColor: '#FFF2E8',
    color: '#FA541C',
  },
  icon: {
    display: 'inline-flex',
    alignItems: 'center',
    marginRight: '4px',
    flexShrink: '0',
  },
  close: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: '4px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    backgroundColor: 'transparent',
    color: 'inherit',
    cursor: 'pointer',
    opacity: '0.65',
    flexShrink: '0',
    ':hover': {
      opacity: '1',
    },
  },
});

type TagStyleKey = keyof typeof styles;

const PRESET_KEYS: ReadonlySet<string> = new Set([
  'default',
  'success',
  'warning',
  'error',
  'processing',
  'blue',
  'green',
  'red',
  'orange',
  'gold',
  'purple',
  'magenta',
  'geekblue',
  'cyan',
  'volcano',
]);

const HEX6 = /^#[0-9a-fA-F]{6}$/;

export const Tag: FC<TagProps> = ({ color, closable = false, onClose, icon, style, className, children }) => {
  const [hidden, setHidden] = useState(false);
  if (hidden) return null;

  const preset = color == null ? 'default' : PRESET_KEYS.has(color) ? (color as TagStyleKey) : undefined;
  // 动态色值(自定义 hex / 任意色)→ 行内样式:10% tint 底 + 同色文字(antd v6 filled 语义)
  const dynamicStyle: CSSProperties | undefined =
    preset === undefined && color != null
      ? HEX6.test(color)
        ? { backgroundColor: `${color}1A`, color }
        : { backgroundColor: `color-mix(in srgb, ${color} 10%, white)`, color }
      : undefined;

  const sp = stylex.props(styles.base, styles[preset ?? 'default']);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = { ...sp.style, ...dynamicStyle, ...style };

  const handleClose = (e: MouseEvent<HTMLElement>) => {
    e.stopPropagation();
    onClose?.(e);
    if (!e.defaultPrevented) setHidden(true);
  };

  return (
    <span className={mergedClassName} style={mergedStyle}>
      {icon != null && <span {...stylex.props(styles.icon)}>{icon}</span>}
      {children}
      {closable && (
        <button type="button" aria-label="关闭" {...stylex.props(styles.close)} onClick={handleClose}>
          <CloseOutlined />
        </button>
      )}
    </span>
  );
};

export default Tag;
