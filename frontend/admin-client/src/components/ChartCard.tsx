import React from 'react';
import { Card, Spin, Empty } from 'antd';
import { ResponsiveContainer } from 'recharts';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  empty: {
    padding: '40px',
  },
});

export interface ChartCardProps {
  title: React.ReactNode;
  /** 卡片右上角操作(如高峰时段标注、商品选择器等) */
  extra?: React.ReactNode;
  loading?: boolean;
  /** 图表高度(px,默认 300) */
  height?: number;
  /** 是否展示空态(替代图表) */
  empty?: boolean;
  /** 空态文案 */
  emptyText?: string;
  size?: 'small' | 'default';
  /** 透传给 Card 的样式(如 marginTop/height:100%) */
  style?: React.CSSProperties;
  /** 透传给 Card 的类名(stylex 样式类经此挂载) */
  className?: string;
  /** 图表下方补充内容(在 Spin 内渲染,如「合计: N 单」) */
  footer?: React.ReactNode;
  children: React.ReactNode;
}

/**
 * 图表卡片外壳 —— 统一 Card + Spin + Empty + ResponsiveContainer 样板。
 * 各统计图表只关心自己的图表配置/色板/format,外壳一律经本组件渲染。
 */
const ChartCard: React.FC<ChartCardProps> = ({
  title,
  extra,
  loading = false,
  height = 300,
  empty = false,
  emptyText = '暂无数据',
  size,
  style,
  className,
  footer,
  children,
}) => (
  <Card title={title} extra={extra} size={size} style={style} className={className}>
    <Spin spinning={loading}>
      {empty ? (
        <Empty description={emptyText} {...stylex.props(styles.empty)} />
      ) : (
        <>
          <ResponsiveContainer width="100%" height={height}>
            {children}
          </ResponsiveContainer>
          {footer}
        </>
      )}
    </Spin>
  </Card>
);

export default ChartCard;
