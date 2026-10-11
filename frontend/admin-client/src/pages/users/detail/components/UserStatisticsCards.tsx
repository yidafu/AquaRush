import React from 'react';
import { Row, Col } from '@/ui';
import { ShoppingOutlined, DollarOutlined } from '@/ui/icons';
import StatisticsCard from '../../../../components/StatisticsCard';
import { BRAND_PRIMARY, COLOR_ERROR, COLOR_SUCCESS } from '../../../../config/theme';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  row: {
    marginBottom: '24px',
  },
});

export interface UserStatisticsCardsProps {
  totalOrders: number;
  /** 总消费金额(元) */
  totalAmountInYuan: number;
  completedOrders: number;
  /** 平均订单金额(元) */
  averageAmountInYuan: number;
}

/** 用户详情统计卡(全量口径,users/detail/index.tsx 拆分;复用 StatisticsCard 外壳) */
const UserStatisticsCards: React.FC<UserStatisticsCardsProps> = ({
  totalOrders,
  totalAmountInYuan,
  completedOrders,
  averageAmountInYuan,
}) => (
  <Row {...stylex.props(styles.row)} gutter={16}>
    <Col span={6}>
      <StatisticsCard
        title="总订单数"
        value={totalOrders}
        icon={<ShoppingOutlined />}
        valueStyle={{ color: BRAND_PRIMARY }}
      />
    </Col>
    <Col span={6}>
      <StatisticsCard
        title="总消费金额"
        value={totalAmountInYuan}
        icon={<DollarOutlined />}
        precision={2}
        valueStyle={{ color: COLOR_ERROR }}
        suffix="元"
      />
    </Col>
    <Col span={6}>
      <StatisticsCard
        title="已完成订单"
        value={completedOrders}
        icon={<ShoppingOutlined />}
        valueStyle={{ color: COLOR_SUCCESS }}
      />
    </Col>
    <Col span={6}>
      <StatisticsCard
        title="平均订单金额"
        value={averageAmountInYuan}
        icon={<DollarOutlined />}
        precision={2}
        suffix="元"
        valueStyle={{ color: COLOR_ERROR }}
      />
    </Col>
  </Row>
);

export default UserStatisticsCards;
