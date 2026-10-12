import { useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as SwitchPrimitive from '@radix-ui/react-switch';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { SyncOutlined } from './icons';

export interface SwitchProps {
  checked?: boolean;
  defaultChecked?: boolean;
  /** antd 签名:onChange(checked: boolean, event) */
  onChange?: (checked: boolean) => void;
  checkedChildren?: ReactNode;
  unCheckedChildren?: ReactNode;
  disabled?: boolean;
  size?: 'small' | 'default';
  /** 上传/提交中态:禁用并展示旋转图标(antd Switch loading) */
  loading?: boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

const styles = stylex.create({
  root: {
    position: 'relative',
    display: 'inline-flex',
    flexShrink: '0',
    alignItems: 'center',
    width: '44px',
    height: '22px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: '11px',
    // 开关底色为状态依赖值,保留 style={{ }} 动态注入
    cursor: 'pointer',
    transitionProperty: 'background-color',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    verticalAlign: 'middle',
  },
  rootSmall: {
    width: '28px',
    height: '16px',
    borderRadius: '8px',
  },
  rootDisabled: {
    cursor: 'not-allowed',
    opacity: '0.5',
  },
  thumb: {
    display: 'block',
    pointerEvents: 'none',
    width: '18px',
    height: '18px',
    backgroundColor: '#FFFFFF',
    borderRadius: '50%',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.15)',
    transitionProperty: 'transform',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    // 位移依赖开关状态,保留 style={{ }} 动态注入
  },
  thumbSmall: {
    width: '12px',
    height: '12px',
  },
  label: {
    marginLeft: '0px',
    marginRight: '0px',
    paddingLeft: '6px',
    paddingRight: '6px',
    fontSize: '12px',
    lineHeight: '16px',
    whiteSpace: 'nowrap',
    // 文字颜色依赖开关状态,保留 style={{ }} 动态注入
  },
  wrapper: {
    display: 'inline-flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '0px',
    rowGap: '0px',
  },
  loadingIcon: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    fontSize: '10px',
    color: tokens.onBrand,
    pointerEvents: 'none',
  },
});

export const Switch: FC<SwitchProps> = ({
  checked,
  defaultChecked = false,
  onChange,
  checkedChildren,
  unCheckedChildren,
  disabled = false,
  size = 'default',
  loading = false,
  className,
  style,
}) => {
  const [innerChecked, setInnerChecked] = useState(defaultChecked);
  const isControlled = checked !== undefined;
  const on = isControlled ? checked : innerChecked;
  const isSmall = size === 'small';
  const hasLabel = checkedChildren != null || unCheckedChildren != null;

  const handleChange = (next: boolean) => {
    if (disabled || loading) return;
    if (!isControlled) setInnerChecked(next);
    onChange?.(next);
  };

  const rootStyle: CSSProperties = {
    // 动态:开关底色随状态切换
    backgroundColor: on ? tokens.brand : 'rgba(0, 0, 0, 0.22)',
  };
  const thumbStyle: CSSProperties = {
    // 动态:thumb 位移随状态切换(Radix 不注入 transform;大号 44-18-4=22px,小号 28-12-2=14px)
    transform: on ? `translateX(${isSmall ? 14 : 22}px)` : 'translateX(2px)',
  };
  const labelStyle: CSSProperties = {
    // 动态:文字颜色随状态切换
    color: on ? tokens.brand : tokens.textSub,
  };

  return (
    <span {...stylex.props(styles.wrapper)} className={className} style={style}>
      <SwitchPrimitive.Root
        checked={on}
        onCheckedChange={handleChange}
        disabled={disabled || loading}
        {...stylex.props(styles.root, isSmall && styles.rootSmall, (disabled || loading) && styles.rootDisabled)}
        style={rootStyle}
      >
        <SwitchPrimitive.Thumb
          {...stylex.props(styles.thumb, isSmall && styles.thumbSmall)}
          style={thumbStyle}
        />
        {loading && (
          <span {...stylex.props(styles.loadingIcon)}>
            <SyncOutlined spin />
          </span>
        )}
      </SwitchPrimitive.Root>
      {hasLabel && (
        <span {...stylex.props(styles.label)} style={labelStyle}>
          {on ? checkedChildren : unCheckedChildren}
        </span>
      )}
    </span>
  );
};
