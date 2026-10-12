import { useState } from 'react';
import type { CSSProperties, FC } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { StarOutlined } from './icons';

export interface RateProps {
  value?: number;
  defaultValue?: number;
  onChange?: (value: number) => void;
  count?: number;
  disabled?: boolean;
  className?: string;
  style?: CSSProperties;
}

const styles = stylex.create({
  root: {
    display: 'inline-flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '4px',
    rowGap: '4px',
    verticalAlign: 'middle',
  },
  rootDisabled: {
    cursor: 'default',
  },
  star: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    // 颜色依赖评分状态,保留 style={{ }} 动态注入
    fontSize: '20px',
    lineHeight: '1',
    cursor: 'pointer',
  },
  starDisabled: {
    cursor: 'default',
  },
});

export const Rate: FC<RateProps> = ({
  value,
  defaultValue,
  onChange,
  count = 5,
  disabled = false,
  className,
  style,
}) => {
  const [innerValue, setInnerValue] = useState(defaultValue ?? 0);
  // 受控优先;value 缺省时使用内部状态(antd 语义)
  const current = value !== undefined ? value : innerValue;
  const [hoverValue, setHoverValue] = useState<number | null>(null);
  const shown = hoverValue ?? current;

  const select = (next: number) => {
    if (disabled) return;
    if (value === undefined) setInnerValue(next);
    onChange?.(next);
  };

  const rootCx = stylex.props(styles.root, disabled && styles.rootDisabled);

  return (
    <div
      role="radiogroup"
      aria-readonly={disabled}
      {...rootCx}
      className={[rootCx.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
      onMouseLeave={() => setHoverValue(null)}
    >
      {Array.from({ length: count }, (_, index) => {
        const starValue = index + 1;
        const filled = starValue <= shown;
        return (
          <button
            key={starValue}
            type="button"
            role="radio"
            aria-checked={starValue === current}
            aria-label={`${starValue} 星`}
            disabled={disabled}
            onClick={() => select(starValue)}
            onMouseEnter={() => {
              if (!disabled) setHoverValue(starValue);
            }}
            {...stylex.props(styles.star, disabled && styles.starDisabled)}
            // 动态:已选星为金色,未选为中性灰(token)
            style={{ color: filled ? tokens.warning : tokens.borderStrong }}
          >
            <StarOutlined />
          </button>
        );
      })}
    </div>
  );
};
