import { createContext, useContext, useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as RadioGroupPrimitive from '@radix-ui/react-radio-group';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

/** antd RadioChangeEvent 子集(target.value 供 Form.Item 提取) */
export interface RadioChangeEvent {
  target: {
    value: unknown;
  };
}

export interface RadioProps {
  value?: string;
  checked?: boolean;
  defaultChecked?: boolean;
  onChange?: (e: RadioChangeEvent) => void;
  disabled?: boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

export interface RadioOption {
  label: ReactNode;
  value: string;
  disabled?: boolean;
}

export interface RadioGroupProps {
  options?: Array<RadioOption | string>;
  value?: string;
  defaultValue?: string;
  /** antd 签名:事件对象,取 e.target.value */
  onChange?: (e: RadioChangeEvent) => void;
  disabled?: boolean;
  name?: string;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

interface GroupContextValue {
  /** 标记位于 Radix RadioGroup 内:Radio 渲染为 Radix Item(获得方向键导航) */
  inGroup: true;
  value?: string;
  disabled?: boolean;
}

const GroupContext = createContext<GroupContextValue | null>(null);

const styles = stylex.create({
  item: {
    display: 'inline-flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    cursor: 'pointer',
    verticalAlign: 'middle',
    fontSize: '14px',
    lineHeight: '20px',
  },
  itemDisabled: {
    cursor: 'not-allowed',
    opacity: '0.5',
  },
  circle: {
    position: 'relative',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '50%',
    // 边框色依赖选中状态,保留 style={{ }} 动态注入
    flexShrink: '0',
    transitionProperty: 'border-color',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
  },
  dot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    // 底色依赖选中状态,保留 style={{ }} 动态注入
  },
  label: {
    color: tokens.text,
    cursor: 'inherit',
    textAlign: 'left',
  },
  group: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    columnGap: '16px',
    rowGap: '8px',
  },
});

const renderVisual = (on: boolean, disabled: boolean): ReactNode => (
  <>
    <span
      {...stylex.props(styles.circle)}
      // 动态:选中态边框随状态切换
      style={{ borderColor: on ? tokens.brand : disabled ? tokens.textWeak : 'rgba(0, 0, 0, 0.25)' }}
    >
      {on && <span {...stylex.props(styles.dot)} style={{ backgroundColor: tokens.brand }} />}
    </span>
  </>
);

const RadioImpl: FC<RadioProps> = ({
  value,
  checked,
  defaultChecked = false,
  onChange,
  disabled,
  className,
  style,
  children,
}) => {
  const group = useContext(GroupContext);
  const isGroupMember = group !== null && value !== undefined;
  const isDisabled = disabled ?? group?.disabled ?? false;

  const [innerChecked, setInnerChecked] = useState(defaultChecked);
  const on = isGroupMember
    ? group.value === value
    : checked !== undefined
      ? checked
      : innerChecked;

  const handleClick = () => {
    if (isDisabled) return;
    if (checked === undefined) setInnerChecked(true);
    onChange?.({ target: { value } });
  };

  // 组内:渲染 Radix Item(Root 的 onValueChange 统一回调,方向键导航由 Radix 提供)
  if (isGroupMember && value !== undefined) {
    return (
      <RadioGroupPrimitive.Item
        value={value}
        disabled={isDisabled}
        {...stylex.props(styles.item, isDisabled && styles.itemDisabled)}
        className={className}
        style={style}
      >
        {renderVisual(on, isDisabled)}
        {children != null && <span {...stylex.props(styles.label)}>{children}</span>}
      </RadioGroupPrimitive.Item>
    );
  }

  return (
    <button
      type="button"
      role="radio"
      aria-checked={on}
      disabled={isDisabled}
      onClick={handleClick}
      {...stylex.props(styles.item, isDisabled && styles.itemDisabled)}
      className={className}
      style={style}
    >
      {renderVisual(on, isDisabled)}
      {children != null && <span {...stylex.props(styles.label)}>{children}</span>}
    </button>
  );
};

export const RadioGroup: FC<RadioGroupProps> = ({
  options,
  value,
  defaultValue,
  onChange,
  disabled = false,
  name,
  className,
  style,
  children,
}) => {
  const [innerValue, setInnerValue] = useState<string | undefined>(defaultValue);
  const current = value !== undefined ? value : innerValue;

  const handleValueChange = (next: string) => {
    if (value === undefined) setInnerValue(next);
    onChange?.({ target: { value: next } });
  };

  const contextValue: GroupContextValue = { inGroup: true, value: current, disabled };

  const normalizedOptions =
    options?.map((opt) =>
      typeof opt === 'string' ? ({ label: opt, value: opt } as RadioOption) : opt,
    ) ?? [];

  const items =
    normalizedOptions.length > 0
      ? normalizedOptions.map((opt) => (
          <Radio
            key={opt.value}
            value={opt.value}
            disabled={disabled || opt.disabled}
            checked={current === opt.value}
          >
            {opt.label}
          </Radio>
        ))
      : children;

  return (
    <GroupContext.Provider value={contextValue}>
      <RadioGroupPrimitive.Root
        name={name}
        value={current}
        onValueChange={handleValueChange}
        disabled={disabled}
        {...stylex.props(styles.group)}
        className={className}
        style={style}
      >
        {items}
      </RadioGroupPrimitive.Root>
    </GroupContext.Provider>
  );
};

/** antd 同形静态:Radio.Group —— 调用点 `Radio.Group` 无需改写。 */
export const Radio = Object.assign(RadioImpl, { Group: RadioGroup });
