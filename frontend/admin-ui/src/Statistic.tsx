import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export type StatisticValue = number | string;

export interface StatisticProps {
  title?: ReactNode;
  value?: StatisticValue;
  /** 小数位(value 为 number 时 toFixed) */
  precision?: number;
  prefix?: ReactNode;
  suffix?: ReactNode;
  /** 自定义数值渲染(拿到原始 value) */
  formatter?: (value: StatisticValue) => ReactNode;
  /** 覆盖整个数值区(优先于 formatter) */
  valueRender?: () => ReactNode;
  valueStyle?: CSSProperties;
  style?: CSSProperties;
  className?: string;
}

const styles = stylex.create({
  root: {
    boxSizing: 'border-box',
  },
  title: {
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.textSub,
    paddingBottom: '4px',
  },
  content: {
    fontSize: '24px',
    lineHeight: '32px',
    color: tokens.text,
    // 金额/数值等宽,避免刷新跳动(同 index.css .num 语义)
    fontVariantNumeric: 'tabular-nums',
    wordBreak: 'break-word',
  },
  value: {
    display: 'inline-block',
    direction: 'ltr',
  },
  affix: {
    display: 'inline-block',
  },
  prefix: {
    marginRight: '4px',
  },
  suffix: {
    marginLeft: '4px',
  },
});

export const Statistic: FC<StatisticProps> = ({
  title,
  value,
  precision,
  prefix,
  suffix,
  formatter,
  valueRender,
  valueStyle,
  style,
  className,
}) => {
  const sp = stylex.props(styles.root);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  let valueNode: ReactNode;
  if (valueRender) {
    valueNode = valueRender();
  } else if (formatter) {
    valueNode = formatter(value ?? 0);
  } else if (typeof value === 'number' && precision != null) {
    valueNode = value.toFixed(precision);
  } else {
    valueNode = value;
  }

  return (
    <div className={mergedClassName} style={mergedStyle}>
      {title != null && <div {...stylex.props(styles.title)}>{title}</div>}
      <div {...stylex.props(styles.content)} style={valueStyle}>
        {prefix != null && <span {...stylex.props(styles.affix, styles.prefix)}>{prefix}</span>}
        <span {...stylex.props(styles.value)}>{valueNode}</span>
        {suffix != null && <span {...stylex.props(styles.affix, styles.suffix)}>{suffix}</span>}
      </div>
    </div>
  );
};

export default Statistic;
