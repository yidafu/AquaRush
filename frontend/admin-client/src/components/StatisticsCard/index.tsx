import React from 'react';
import { Card, Statistic } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';

interface StatisticsCardProps {
  title: string;
  value: number | string;
  prefix?: React.ReactNode;
  suffix?: string;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: number;
  loading?: boolean;
}

const StatisticsCard: React.FC<StatisticsCardProps> = ({
  title,
  value,
  prefix,
  suffix,
  trend = 'neutral',
  trendValue,
  loading = false,
}) => {
  const getTrendIcon = () => {
    if (trend === 'up') return <ArrowUpOutlined />;
    if (trend === 'down') return <ArrowDownOutlined />;
    return null;
  };

  const getTrendColor = () => {
    if (trend === 'up') return '#3f8600';
    if (trend === 'down') return '#cf1322';
    return '#999';
  };

  return (
    <Card loading={loading}>
      <Statistic
        title={title}
        value={value}
        prefix={prefix}
        suffix={suffix}
        valueStyle={{ color: getTrendColor() }}
        prefix={trend !== 'neutral' ? getTrendIcon() : prefix}
      />
      {trendValue !== undefined && trend !== 'neutral' && (
        <span style={{ color: getTrendColor(), fontSize: 14 }}>
          {trendValue > 0 ? '+' : ''}
          {trendValue}% 较昨日
        </span>
      )}
    </Card>
  );
};

export default StatisticsCard;
