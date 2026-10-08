import React from 'react';
import { Row, Col } from 'antd';
import { ShoppingOutlined, DollarOutlined } from '@ant-design/icons';
import StatisticsCard from '../../../../components/StatisticsCard';
import { BRAND_PRIMARY } from '../../../../config/theme';

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
  <Row gutter={16} style={{ marginBottom: '24px' }}>
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
        valueStyle={{ color: '#f5222d' }}
        suffix="元"
      />
    </Col>
    <Col span={6}>
      <StatisticsCard
        title="已完成订单"
        value={completedOrders}
        icon={<ShoppingOutlined />}
        valueStyle={{ color: '#52c41a' }}
      />
    </Col>
    <Col span={6}>
      <StatisticsCard
        title="平均订单金额"
        value={averageAmountInYuan}
        icon={<DollarOutlined />}
        precision={2}
        suffix="元"
        valueStyle={{ color: '#f5222d' }}
      />
    </Col>
  </Row>
);

export default UserStatisticsCards;
