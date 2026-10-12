import { useRef, useState } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface SegmentedOption<T = string> {
  label: ReactNode;
  value: T;
  disabled?: boolean;
}

export interface SegmentedProps<T = string> {
  options: Array<SegmentedOption<T> | string>;
  value?: T;
  defaultValue?: T;
  onChange?: (value: T) => void;
  size?: 'small' | 'middle' | 'large';
  disabled?: boolean;
  block?: boolean;
  className?: string;
  style?: CSSProperties;
}

const styles = stylex.create({
  root: {
    display: 'inline-flex',
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: '2px',
    paddingBottom: '2px',
    paddingLeft: '2px',
    paddingRight: '2px',
    backgroundColor: tokens.border,
    borderRadius: tokens.rSm,
  },
  rootDisabled: {
    opacity: '0.6',
  },
  rootBlock: {
    display: 'flex',
    width: '100%',
  },
  item: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '3px',
    paddingBottom: '3px',
    paddingLeft: '12px',
    paddingRight: '12px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: '6px',
    backgroundColor: 'transparent',
    color: tokens.textSub,
    fontSize: '14px',
    lineHeight: '20px',
    fontWeight: '400',
    whiteSpace: 'nowrap',
    cursor: 'pointer',
    userSelect: 'none',
    // 选中态:白底 + 品牌色文字(antd Segmented 基线,颜色向设计规范微调)
    ':hover': {
      color: tokens.text,
    },
  },
  itemActive: {
    backgroundColor: tokens.card,
    color: tokens.brand,
    fontWeight: '500',
    boxShadow: '0 1px 2px rgba(0, 0, 0, 0.06)',
    ':hover': {
      color: tokens.brand,
    },
  },
  itemDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      color: tokens.textWeak,
    },
  },
  itemSmall: {
    paddingTop: '1px',
    paddingBottom: '1px',
    paddingLeft: '8px',
    paddingRight: '8px',
    fontSize: '12px',
    lineHeight: '18px',
  },
  itemLarge: {
    paddingTop: '5px',
    paddingBottom: '5px',
    paddingLeft: '16px',
    paddingRight: '16px',
    fontSize: '15px',
    lineHeight: '22px',
  },
});

const normalize = <T,>(options: Array<SegmentedOption<T> | string>): Array<SegmentedOption<T>> =>
  options.map((opt) => (typeof opt === 'string' ? ({ label: opt, value: opt } as SegmentedOption<T>) : opt));

export function Segmented<T = string>({
  options,
  value,
  defaultValue,
  onChange,
  size = 'middle',
  disabled = false,
  block = false,
  className,
  style,
}: SegmentedProps<T>): ReactNode {
  const normalized = normalize(options);
  const [innerValue, setInnerValue] = useState<T | undefined>(() => defaultValue ?? normalized[0]?.value);
  const current = value !== undefined ? value : innerValue;
  const rootRef = useRef<HTMLDivElement | null>(null);

  const select = (next: T) => {
    if (disabled) return;
    if (value === undefined) setInnerValue(next);
    onChange?.(next);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
    event.preventDefault();
    const enabled = normalized.filter((opt) => !opt.disabled);
    if (enabled.length === 0) return;
    const currentIndex = enabled.findIndex((opt) => opt.value === current);
    const delta = event.key === 'ArrowRight' ? 1 : -1;
    const nextIndex = (currentIndex + delta + enabled.length) % enabled.length;
    const next = enabled[nextIndex === -1 ? 0 : nextIndex];
    select(next.value);
    const buttons = rootRef.current?.querySelectorAll<HTMLButtonElement>('button:not([disabled])');
    buttons?.[Math.max(0, nextIndex)]?.focus();
  };

  const sizeClass = size === 'small' ? styles.itemSmall : size === 'large' ? styles.itemLarge : null;

  return (
    <div
      ref={rootRef}
      role="radiogroup"
      {...stylex.props(
        styles.root,
        disabled && styles.rootDisabled,
        block && styles.rootBlock,
      )}
      className={className}
      style={style}
      onKeyDown={handleKeyDown}
    >
      {normalized.map((opt) => {
        const active = opt.value === current;
        return (
          <button
            key={String(opt.value)}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={disabled || opt.disabled}
            onClick={() => select(opt.value)}
            {...stylex.props(
              styles.item,
              sizeClass,
              active && styles.itemActive,
              opt.disabled && styles.itemDisabled,
            )}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
