import React from 'react';
import { Card, Statistic } from '@aquarush/admin-ui';
import { ArrowUpOutlined, ArrowDownOutlined } from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';
import { COLOR_SUCCESS, COLOR_ERROR, TEXT_SECONDARY } from '../../config/theme';

// 趋势色依赖 trend props,保留 style={{ color: getTrendColor() }} 动态注入
const styles = stylex.create({
  trendLine: {
    fontSize: '14px',
  },
});

interface StatisticsCardProps {
  title: string;
  value: number | string;
  /** 统计项图标(渲染为 Statistic prefix;未提供时可用 prefix) */
  icon?: React.ReactNode;
  prefix?: React.ReactNode;
  suffix?: string;
  precision?: number;
  size?: 'small' | 'default';
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: number | null;
  trendLabel?: string;
  loading?: boolean;
  /** 覆盖默认趋势色(如需要品牌色/红绿强调时传入) */
  valueStyle?: React.CSSProperties;
}

const StatisticsCard: React.FC<StatisticsCardProps> = ({
  title,
  value,
  icon,
  prefix,
  suffix,
  precision,
  size,
  trend = 'neutral',
  trendValue,
  trendLabel = '较昨日',
  loading = false,
  valueStyle,
}) => {
  const getTrendIcon = () => {
    if (trend === 'up') return <ArrowUpOutlined />;
    if (trend === 'down') return <ArrowDownOutlined />;
    return null;
  };

  const getTrendColor = () => {
    if (trend === 'up') return COLOR_SUCCESS; // 涨绿
    if (trend === 'down') return COLOR_ERROR; // 跌红
    return TEXT_SECONDARY;
  };

  const showTrendLine = trendValue !== undefined;

  return (
    <Card loading={loading} size={size}>
      <Statistic
        title={title}
        value={value}
        prefix={prefix ?? icon}
        suffix={suffix}
        precision={precision}
        valueStyle={valueStyle ?? { color: getTrendColor() }}
      />
      {showTrendLine && (
        <span {...stylex.props(styles.trendLine)} style={{ color: getTrendColor() }}>
          {trendValue === null || trendValue === undefined || !Number.isFinite(trendValue) ? (
            '—'
          ) : (
            <>
              {getTrendIcon()}
              {trendValue > 0 ? '+' : ''}
              {trendValue.toFixed(1)}% {trendLabel}
            </>
          )}
        </span>
      )}
    </Card>
  );
};

export default StatisticsCard;
