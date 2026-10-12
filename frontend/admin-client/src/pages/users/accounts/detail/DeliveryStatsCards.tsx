import React from 'react';
import { Card, Col, Row, Statistic } from '@aquarush/admin-ui';
import {
  ShoppingOutlined,
  CheckCircleOutlined,
  StarOutlined,
  DollarOutlined,
} from '@aquarush/admin-ui/icons';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { BRAND_PRIMARY, COLOR_WARNING, COLOR_SUCCESS } from '../../../../config/theme';

const styles = stylex.create({
  row: {
    marginBottom: '24px',
  },
});

interface DeliveryStatsCardsProps {
  totalOrders: number;
  completedOrders: number;
  averageRating?: number | null;
  earningCents?: number | null;
}

/**
 * 配送统计卡(账号详情页共享组件):
 * 总订单数/已完成订单/平均评分/累计收入。
 * 「总评价数/评分分布」后端类型未暴露(数据在 delivery_worker_statistics 表),暂不展示。
 */
const DeliveryStatsCards: React.FC<DeliveryStatsCardsProps> = ({
  totalOrders,
  completedOrders,
  averageRating,
  earningCents,
}) => {
  return (
    <Row gutter={16} {...stylex.props(styles.row)}>
      <Col span={6}>
        <Card>
          <Statistic
            title="总订单数"
            value={totalOrders}
            prefix={<ShoppingOutlined />}
            valueStyle={{ color: BRAND_PRIMARY }}
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="已完成订单"
            value={completedOrders}
            prefix={<CheckCircleOutlined />}
            valueStyle={{ color: COLOR_SUCCESS }}
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="平均评分"
            value={averageRating ?? 0}
            prefix={<StarOutlined />}
            precision={2}
            valueStyle={{ color: COLOR_WARNING }}
            suffix="/ 5.0"
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="累计收入"
            value={earningCents ?? 0}
            prefix={<DollarOutlined />}
            /* valueStyle 落在 .ant-statistic 内部元素,stylex 无法表达;
               #722ed1 为累计收入强调紫,token 无对应值,保留字面量(报告 (a)) */
            valueStyle={{ color: '#722ed1' }}
            formatter={(value) => displayCents(Number(value))}
          />
        </Card>
      </Col>
    </Row>
  );
};

export default DeliveryStatsCards;
