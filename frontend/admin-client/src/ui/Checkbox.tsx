import { createContext, useContext, useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as CheckboxPrimitive from '@radix-ui/react-checkbox';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { CheckOutlined } from './icons';

/** antd CheckboxChangeEvent 子集(target.checked 供 Form.Item valuePropName="checked" 提取) */
export interface CheckboxChangeEvent {
  target: {
    checked: boolean;
    value?: unknown;
  };
}

export interface CheckboxProps {
  checked?: boolean;
  defaultChecked?: boolean;
  onChange?: (e: CheckboxChangeEvent) => void;
  disabled?: boolean;
  value?: string | number | boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

export interface CheckboxOption {
  label: ReactNode;
  value: string | number | boolean;
  disabled?: boolean;
}

export interface CheckboxGroupProps {
  options?: Array<CheckboxOption | string>;
  value?: Array<string | number | boolean>;
  defaultValue?: Array<string | number | boolean>;
  /** antd 签名:直接回传选中值数组(非事件) */
  onChange?: (checkedValue: Array<string | number | boolean>) => void;
  disabled?: boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

interface GroupContextValue {
  value: Array<string | number | boolean>;
  toggle: (optionValue: string | number | boolean, checked: boolean) => void;
  disabled?: boolean;
}

const GroupContext = createContext<GroupContextValue | null>(null);

const styles = stylex.create({
  root: {
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
  rootDisabled: {
    cursor: 'not-allowed',
    opacity: '0.5',
  },
  box: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '4px',
    // 底色/边框色依赖选中状态,保留 style={{ }} 动态注入
    flexShrink: '0',
    color: tokens.onBrand,
  },
  indicator: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '10px',
    lineHeight: '1',
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
    columnGap: '16px',
    rowGap: '8px',
  },
});

export const Checkbox: FC<CheckboxProps> = ({
  checked,
  defaultChecked = false,
  onChange,
  disabled,
  value,
  className,
  style,
  children,
}) => {
  const group = useContext(GroupContext);
  const isGroupMember = group !== null && value !== undefined;
  const isDisabled = disabled ?? group?.disabled ?? false;

  const [innerChecked, setInnerChecked] = useState(defaultChecked);
  const on = isGroupMember
    ? group.value.includes(value)
    : checked !== undefined
      ? checked
      : innerChecked;

  const handleChange = (next: boolean) => {
    if (isGroupMember) {
      group.toggle(value, next);
      return;
    }
    if (checked === undefined) setInnerChecked(next);
    onChange?.({ target: { checked: next, value } });
  };

  return (
    <CheckboxPrimitive.Root
      checked={on}
      onCheckedChange={handleChange}
      disabled={isDisabled}
      {...stylex.props(styles.root, isDisabled && styles.rootDisabled)}
      className={className}
      style={style}
    >
      <span
        {...stylex.props(styles.box)}
        // 动态:选中态底色/边框随状态切换
        style={{
          backgroundColor: on ? tokens.brand : tokens.card,
          borderColor: on ? tokens.brand : isDisabled ? tokens.textWeak : 'rgba(0, 0, 0, 0.25)',
        }}
      >
        <CheckboxPrimitive.Indicator {...stylex.props(styles.indicator)}>
          <CheckOutlined />
        </CheckboxPrimitive.Indicator>
      </span>
      {children != null && <span {...stylex.props(styles.label)}>{children}</span>}
    </CheckboxPrimitive.Root>
  );
};

export const CheckboxGroup: FC<CheckboxGroupProps> = ({
  options,
  value,
  defaultValue,
  onChange,
  disabled = false,
  className,
  style,
  children,
}) => {
  const [innerValue, setInnerValue] = useState<Array<string | number | boolean>>(() => defaultValue ?? []);
  const current = value ?? innerValue;

  const toggle = (optionValue: string | number | boolean, checked: boolean) => {
    const next = checked
      ? [...current, optionValue]
      : current.filter((v) => v !== optionValue);
    if (value === undefined) setInnerValue(next);
    onChange?.(next);
  };

  const contextValue: GroupContextValue = { value: current, toggle, disabled };

  const normalizedOptions =
    options?.map((opt) =>
      typeof opt === 'string' ? ({ label: opt, value: opt } as CheckboxOption) : opt,
    ) ?? [];

  return (
    <GroupContext.Provider value={contextValue}>
      <div {...stylex.props(styles.group)} className={className} style={style}>
        {normalizedOptions.length > 0
          ? normalizedOptions.map((opt) => (
              <Checkbox
                key={String(opt.value)}
                value={opt.value}
                disabled={disabled || opt.disabled}
                checked={current.includes(opt.value)}
                onChange={(e) => toggle(opt.value, e.target.checked)}
              >
                {opt.label}
              </Checkbox>
            ))
          : children}
      </div>
    </GroupContext.Provider>
  );
};
