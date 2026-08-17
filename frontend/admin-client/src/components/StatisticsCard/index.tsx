import React from 'react';
import { Card, Statistic } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { COLOR_SUCCESS, COLOR_ERROR, TEXT_SECONDARY } from '../../config/theme';

interface StatisticsCardProps {
  title: string;
  value: number | string;
  prefix?: React.ReactNode;
  suffix?: string;
  precision?: number;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: number | null;
  trendLabel?: string;
  loading?: boolean;
}

const StatisticsCard: React.FC<StatisticsCardProps> = ({
  title,
  value,
  prefix,
  suffix,
  precision,
  trend = 'neutral',
  trendValue,
  trendLabel = '较昨日',
  loading = false,
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
    <Card loading={loading}>
      <Statistic
        title={title}
        value={value}
        prefix={prefix}
        suffix={suffix}
        precision={precision}
        valueStyle={{ color: getTrendColor() }}
      />
      {showTrendLine && (
        <span style={{ color: getTrendColor(), fontSize: 14 }}>
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
