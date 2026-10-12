import type { CSSProperties, FC } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface ProgressProps {
  percent?: number;
  status?: 'success' | 'normal' | 'exception' | 'active';
  showInfo?: boolean;
  type?: 'line' | 'circle';
  size?: 'small' | 'default' | 'large';
  className?: string;
  style?: CSSProperties;
}

const styles = stylex.create({
  rootLine: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    width: '100%',
  },
  track: {
    position: 'relative',
    flexGrow: '1',
    height: '8px',
    backgroundColor: tokens.border,
    borderRadius: '4px',
    overflow: 'hidden',
    // 填充宽度/颜色依赖 percent/status,保留 style={{ }} 动态注入
  },
  trackSmall: {
    height: '6px',
    borderRadius: '3px',
  },
  fill: {
    height: '100%',
    // 宽度依赖 percent,保留 style={{ }} 动态注入
    borderRadius: '4px',
    transitionProperty: 'width',
    transitionDuration: '0.3s',
    transitionTimingFunction: 'ease',
  },
  fillSmall: {
    borderRadius: '3px',
  },
  info: {
    flexShrink: '0',
    fontSize: '12px',
    lineHeight: '16px',
    // 颜色依赖 status,保留 style={{ }} 动态注入
    minWidth: '32px',
    textAlign: 'right',
  },
  rootCircle: {
    position: 'relative',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    // 直径依赖 size,保留 style={{ }} 动态注入
  },
  circleTrack: {
    position: 'absolute',
    top: '0px',
    left: '0px',
    transform: 'rotate(-90deg)',
  },
  circleInfo: {
    position: 'relative',
    fontSize: '14px',
    lineHeight: '20px',
    fontWeight: '600',
    // 颜色依赖 status,保留 style={{ }} 动态注入
  },
});

const clampPercent = (value: number): number => Math.min(100, Math.max(0, value));

const statusColor = (status: ProgressProps['status']): string => {
  switch (status) {
    case 'success':
      return tokens.success;
    case 'exception':
      return tokens.error;
    default:
      return tokens.brand;
  }
};

export const Progress: FC<ProgressProps> = ({
  percent = 0,
  status,
  showInfo = true,
  type = 'line',
  size = 'default',
  className,
  style,
}) => {
  const value = clampPercent(percent);
  const effectiveStatus: ProgressProps['status'] = status ?? 'normal';
  const color = statusColor(effectiveStatus);
  const infoColor =
    effectiveStatus === 'success'
      ? tokens.success
      : effectiveStatus === 'exception'
        ? tokens.error
        : tokens.textSub;
  const infoText = `${Math.round(value)}%`;

  if (type === 'circle') {
    const diameter = size === 'small' ? 64 : size === 'large' ? 132 : 100;
    const strokeWidth = size === 'small' ? 5 : 8;
    const radius = (diameter - strokeWidth) / 2;
    const circumference = 2 * Math.PI * radius;
    const rootCx = stylex.props(styles.rootCircle);

    return (
      <div
        {...rootCx}
        className={[rootCx.className, className].filter(Boolean).join(' ') || undefined}
        // 动态:circle 直径随 size 变化
        style={{ width: diameter, height: diameter, ...style }}
      >
        <svg
          {...stylex.props(styles.circleTrack)}
          width={diameter}
          height={diameter}
          viewBox={`0 0 ${diameter} ${diameter}`}
        >
          <circle
            cx={diameter / 2}
            cy={diameter / 2}
            r={radius}
            fill="none"
            stroke={tokens.border}
            strokeWidth={strokeWidth}
          />
          <circle
            cx={diameter / 2}
            cy={diameter / 2}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            // 动态:进度弧长随 percent 变化
            strokeDashoffset={circumference * (1 - value / 100)}
            // 动态:进度弧长随 percent 变化
            style={{
              transitionProperty: 'stroke-dasharray',
              transitionDuration: '0.3s',
              transitionTimingFunction: 'ease',
            }}
          />
        </svg>
        {showInfo && (
          <span {...stylex.props(styles.circleInfo)} style={{ color: infoColor }}>
            {infoText}
          </span>
        )}
      </div>
    );
  }

  const isSmall = size === 'small';
  const trackCx = stylex.props(styles.track, isSmall && styles.trackSmall);
  const fillCx = stylex.props(styles.fill, isSmall && styles.fillSmall);
  const infoCx = stylex.props(styles.info);
  const rootLineCx = stylex.props(styles.rootLine);

  return (
    <div
      {...rootLineCx}
      className={[rootLineCx.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
    >
      <div {...trackCx}>
        <div
          {...fillCx}
          // 动态:填充宽度随 percent、颜色随 status
          style={{ width: `${value}%`, backgroundColor: color }}
        />
      </div>
      {showInfo && (
        <span {...infoCx} style={{ color: infoColor }}>
          {infoText}
        </span>
      )}
    </div>
  );
};
