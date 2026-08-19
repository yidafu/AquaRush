import React from 'react';
import { Row, Col, Card, Statistic } from 'antd';
import { ShoppingOutlined, DollarOutlined } from '@ant-design/icons';
import { BRAND_PRIMARY } from '../../../../config/theme';

export interface UserStatisticsCardsProps {
  totalOrders: number;
  /** 总消费金额(元) */
  totalAmountInYuan: number;
  completedOrders: number;
  /** 平均订单金额(元) */
  averageAmountInYuan: number;
}

/** 用户详情统计卡(全量口径,users/detail/index.tsx 拆分) */
const UserStatisticsCards: React.FC<UserStatisticsCardsProps> = ({
  totalOrders,
  totalAmountInYuan,
  completedOrders,
  averageAmountInYuan,
}) => (
  <Row gutter={16} style={{ marginBottom: '24px' }}>
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
          title="总消费金额"
          value={totalAmountInYuan}
          prefix={<DollarOutlined />}
          precision={2}
          valueStyle={{ color: '#f5222d' }}
          suffix="元"
        />
      </Card>
    </Col>
    <Col span={6}>
      <Card>
        <Statistic
          title="已完成订单"
          value={completedOrders}
          prefix={<ShoppingOutlined />}
          valueStyle={{ color: '#52c41a' }}
        />
      </Card>
    </Col>
    <Col span={6}>
      <Card>
        <Statistic
          title="平均订单金额"
          value={averageAmountInYuan}
          prefix={<DollarOutlined />}
          precision={2}
          suffix="元"
          valueStyle={{ color: '#f5222d' }}
        />
      </Card>
    </Col>
  </Row>
);

export default UserStatisticsCards;
