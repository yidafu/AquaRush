import type { ButtonHTMLAttributes, ReactNode } from 'react';
import type { FC } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export type ButtonType = 'default' | 'primary' | 'dashed' | 'text' | 'link';
export type ButtonSize = 'small' | 'middle' | 'large';

export interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  type?: ButtonType;
  /** 原生 button type(antd 同名);缺省 'button' */
  htmlType?: 'button' | 'submit' | 'reset';
  size?: ButtonSize;
  danger?: boolean;
  /** antd 同名:透明底描边按钮(现网 primary ghost 一处 + 后续批次) */
  ghost?: boolean;
  block?: boolean;
  loading?: boolean;
  /** 文案前图标节点 */
  icon?: ReactNode;
}

const spinKeyframes = stylex.keyframes({
  from: { transform: 'rotate(0deg)' },
  to: { transform: 'rotate(360deg)' },
});

const styles = stylex.create({
  base: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    columnGap: '8px',
    rowGap: '0px',
    boxSizing: 'border-box',
    position: 'relative',
    whiteSpace: 'nowrap',
    textDecoration: 'none',
    textAlign: 'center',
    cursor: 'pointer',
    userSelect: 'none',
    // 圆角按设计规范「管理端按钮全圆角」(spec/design/ui-design-system.md §3.3)
    borderRadius: tokens.rBtn,
    transitionProperty: 'background-color, border-color, color, opacity',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    paddingTop: '0px',
    paddingBottom: '0px',
    ':focus-visible': {
      outlineStyle: 'solid',
      outlineWidth: '2px',
      outlineColor: tokens.brand,
      outlineOffset: '1px',
    },
    ':active': {
      transform: 'scale(0.98)',
    },
  },
  // —— 尺寸 ——
  small: {
    height: '24px',
    fontSize: '14px',
    lineHeight: '20px',
    paddingLeft: '8px',
    paddingRight: '8px',
  },
  middle: {
    height: '32px',
    fontSize: '14px',
    lineHeight: '20px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  large: {
    height: '40px',
    fontSize: '16px',
    lineHeight: '24px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  // —— 类型 × danger(bg/边框/文字在同一类内一次给齐,避免类间同属性竞争)——
  default: {
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    color: tokens.text,
    ':hover': {
      borderColor: tokens.brand,
      color: tokens.brand,
    },
  },
  defaultDanger: {
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.error,
    color: tokens.error,
    ':hover': {
      backgroundColor: tokens.statusErrorBg,
      borderColor: tokens.error,
      color: tokens.error,
    },
  },
  primary: {
    backgroundColor: tokens.brand,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.brand,
    color: tokens.onBrand,
    ':hover': {
      backgroundColor: tokens.brandDark,
      borderColor: tokens.brandDark,
    },
  },
  primaryDanger: {
    backgroundColor: tokens.error,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.error,
    color: tokens.onBrand,
    ':hover': {
      opacity: '0.88',
    },
  },
  ghostPrimary: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.brand,
    color: tokens.brand,
    ':hover': {
      backgroundColor: tokens.brandTint,
      borderColor: tokens.brand,
      color: tokens.brand,
    },
  },
  ghostPrimaryDanger: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.error,
    color: tokens.error,
    ':hover': {
      backgroundColor: tokens.statusErrorBg,
      borderColor: tokens.error,
      color: tokens.error,
    },
  },
  dashed: {
    backgroundColor: tokens.card,
    borderStyle: 'dashed',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    color: tokens.text,
    ':hover': {
      borderColor: tokens.brand,
      color: tokens.brand,
    },
  },
  dashedDanger: {
    backgroundColor: tokens.card,
    borderStyle: 'dashed',
    borderWidth: '1px',
    borderColor: tokens.error,
    color: tokens.error,
    ':hover': {
      backgroundColor: tokens.statusErrorBg,
      borderColor: tokens.error,
      color: tokens.error,
    },
  },
  text: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '0px',
    borderColor: 'transparent',
    color: tokens.text,
    ':hover': {
      // antd text 按钮 hover 灰底(设计 token 无对应值,保留字面量)
      backgroundColor: 'rgba(0, 0, 0, 0.04)',
    },
  },
  textDanger: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '0px',
    borderColor: 'transparent',
    color: tokens.error,
    ':hover': {
      backgroundColor: tokens.statusErrorBg,
    },
  },
  link: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '0px',
    borderColor: 'transparent',
    color: tokens.brand,
    paddingLeft: '0px',
    paddingRight: '0px',
    ':hover': {
      color: tokens.brandDark,
      textDecoration: 'underline',
    },
  },
  linkDanger: {
    backgroundColor: 'transparent',
    borderStyle: 'solid',
    borderWidth: '0px',
    borderColor: 'transparent',
    color: tokens.error,
    paddingLeft: '0px',
    paddingRight: '0px',
    ':hover': {
      color: tokens.error,
      textDecoration: 'underline',
    },
  },
  // —— 状态(禁用时替换类型类,保证无 hover/active 竞争)——
  disabled: {
    backgroundColor: tokens.bg,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      backgroundColor: tokens.bg,
      borderColor: tokens.border,
      color: tokens.textWeak,
    },
    ':active': {
      transform: 'none',
    },
  },
  loading: {
    cursor: 'default',
    pointerEvents: 'none',
  },
  block: {
    width: '100%',
  },
  icon: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
  },
  loadingIcon: {
    width: '1em',
    height: '1em',
    flexShrink: '0',
    boxSizing: 'border-box',
    borderStyle: 'solid',
    borderWidth: '2px',
    borderTopColor: 'currentColor',
    borderRightColor: 'transparent',
    borderBottomColor: 'transparent',
    borderLeftColor: 'transparent',
    borderRadius: '50%',
    animationName: spinKeyframes,
    animationDuration: '0.8s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
});

function typeStyle(type: ButtonType, danger: boolean, ghost?: boolean) {
  if (ghost === true && (type === 'primary' || type === 'dashed')) {
    return danger ? styles.ghostPrimaryDanger : styles.ghostPrimary;
  }
  if (danger) {
    switch (type) {
      case 'primary':
        return styles.primaryDanger;
      case 'dashed':
        return styles.dashedDanger;
      case 'text':
        return styles.textDanger;
      case 'link':
        return styles.linkDanger;
      default:
        return styles.defaultDanger;
    }
  }
  switch (type) {
    case 'primary':
      return styles.primary;
    case 'dashed':
      return styles.dashed;
    case 'text':
      return styles.text;
    case 'link':
      return styles.link;
    default:
      return styles.default;
  }
}

function sizeStyle(size: ButtonSize) {
  switch (size) {
    case 'small':
      return styles.small;
    case 'large':
      return styles.large;
    default:
      return styles.middle;
  }
}

export const Button: FC<ButtonProps> = ({
  type = 'default',
  htmlType = 'button',
  size = 'middle',
  danger = false,
  ghost = false,
  block = false,
  loading = false,
  icon,
  disabled,
  className,
  style,
  children,
  ...rest
}) => {
  const sp = stylex.props(
    styles.base,
    sizeStyle(size),
    // 禁用态整类替换类型类,避免 hover 规则压过禁用色
    disabled ? styles.disabled : typeStyle(type, danger, ghost),
    block && styles.block,
    loading && styles.loading,
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  return (
    <button type={htmlType} disabled={disabled} className={mergedClassName} style={mergedStyle} {...rest}>
      {loading ? (
        <span {...stylex.props(styles.loadingIcon)} aria-hidden="true" />
      ) : icon ? (
        <span {...stylex.props(styles.icon)}>{icon}</span>
      ) : null}
      {children}
    </button>
  );
};

export default Button;
