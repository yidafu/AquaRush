import { useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as SliderPrimitive from '@radix-ui/react-slider';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface SliderTooltip {
  /** antd tooltip.formatter:返回气泡文案;返回 null/false 隐藏 */
  formatter?: (value?: number) => ReactNode;
}

export interface SliderProps {
  min?: number;
  max?: number;
  step?: number;
  value?: number;
  defaultValue?: number;
  onChange?: (value: number) => void;
  disabled?: boolean;
  /** antd:tooltip={false} 关闭;对象时按 formatter 展示气泡 */
  tooltip?: false | SliderTooltip;
  className?: string;
  style?: CSSProperties;
}

const styles = stylex.create({
  wrapper: {
    display: 'block',
    width: '100%',
  },
  root: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    userSelect: 'none',
    touchAction: 'none',
    width: '100%',
    height: '24px',
  },
  rootDisabled: {
    opacity: '0.5',
  },
  track: {
    position: 'relative',
    flexGrow: '1',
    height: '6px',
    backgroundColor: tokens.border,
    borderRadius: '3px',
    overflow: 'hidden',
  },
  range: {
    position: 'absolute',
    height: '100%',
    // 位置由 Radix 通过内联样式(left/right)计算
    backgroundColor: tokens.brand,
    borderRadius: '3px',
  },
  thumb: {
    position: 'relative',
    width: '16px',
    height: '16px',
    backgroundColor: tokens.card,
    borderRadius: '50%',
    boxShadow: '0 2px 6px rgba(0, 0, 0, 0.2)',
    borderStyle: 'solid',
    borderWidth: '2px',
    borderColor: tokens.brand,
    cursor: 'grab',
    ':active': {
      cursor: 'grabbing',
    },
    ':focus-visible': {
      boxShadow: '0 0 0 4px rgba(58, 155, 217, 0.25)',
    },
  },
  thumbWrap: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  bubble: {
    position: 'absolute',
    bottom: '22px',
    left: '50%',
    transform: 'translateX(-50%)',
    paddingTop: '2px',
    paddingBottom: '2px',
    paddingLeft: '8px',
    paddingRight: '8px',
    whiteSpace: 'nowrap',
    fontSize: '12px',
    lineHeight: '18px',
    color: tokens.onBrand,
    backgroundColor: 'rgba(0, 0, 0, 0.88)',
    borderRadius: '4px',
    pointerEvents: 'none',
    zIndex: '1',
  },
});

export const Slider: FC<SliderProps> = ({
  min = 0,
  max = 100,
  step,
  value,
  defaultValue,
  onChange,
  disabled = false,
  tooltip,
  className,
  style,
}) => {
  const [innerValue, setInnerValue] = useState<number[]>(() => [defaultValue ?? min]);
  const current = value !== undefined ? [value] : innerValue;
  const [active, setActive] = useState(false);
  const [hovered, setHovered] = useState(false);

  const handleChange = (next: number[]) => {
    const single = next[0] ?? min;
    if (value === undefined) setInnerValue([single]);
    onChange?.(single);
  };

  const showBubble = tooltip !== false && (active || hovered);
  const currentValue = current[0] ?? min;
  const bubbleText = tooltip !== false && tooltip?.formatter ? tooltip.formatter(currentValue) : null;
  const wrapCx = stylex.props(styles.wrapper);

  return (
    <div
      {...wrapCx}
      className={[wrapCx.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => {
        setHovered(false);
        setActive(false);
      }}
    >
      <SliderPrimitive.Root
        min={min}
        max={max}
        step={step}
        value={current}
        onValueChange={handleChange}
        onValueCommit={() => setActive(false)}
        disabled={disabled}
        {...stylex.props(styles.root, disabled && styles.rootDisabled)}
        style={{ width: '100%' }}
      >
        <SliderPrimitive.Track {...stylex.props(styles.track)}>
          <SliderPrimitive.Range {...stylex.props(styles.range)} />
        </SliderPrimitive.Track>
        <SliderPrimitive.Thumb
          {...stylex.props(styles.thumb, styles.thumbWrap)}
          onPointerDown={() => setActive(true)}
          onPointerUp={() => setActive(false)}
        >
          {showBubble && bubbleText != null && bubbleText !== false && (
            <span {...stylex.props(styles.bubble)}>{bubbleText}</span>
          )}
        </SliderPrimitive.Thumb>
      </SliderPrimitive.Root>
    </div>
  );
};
