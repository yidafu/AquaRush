import React from 'react';
import { Card, Spin, Row, Col, Statistic } from '@aquarush/admin-ui';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginTop: '24px',
  },
  caption: {
    textAlign: 'center',
    // #999:共享 token 无此色(图注辅助文字灰),按规则保留字面量
    color: '#999',
    fontSize: '13px',
  },
});

export interface OrderTierSlice {
  name: string;
  value: number;
  color: string;
}

export interface OrderUserDistributionCardProps {
  tiers: OrderTierSlice[];
  orderingUsers: number;
  orderingUserRatio: number;
  repurchaseRate: number;
  totalUsers: number;
  loading: boolean;
}

/** 下单用户分布卡(近30天,G35,statistics/users/index.tsx 拆分):
 *  环形图按累计下单次数分层 + 下单用户占比/复购率指标 */
const OrderUserDistributionCard: React.FC<OrderUserDistributionCardProps> = ({
  tiers,
  orderingUsers,
  orderingUserRatio,
  repurchaseRate,
  totalUsers,
  loading,
}) => (
  <Card title="下单用户分布（近30天）" {...stylex.props(styles.card)}>
    <Spin spinning={loading}>
      <Row gutter={[16, 16]} align="middle">
        <Col xs={24} lg={12}>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie
                data={tiers}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={95}
                paddingAngle={2}
              >
                {tiers.map((entry) => (
                  <Cell key={entry.name} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                formatter={(value) => [`${Number(value ?? 0)} 人`, '用户数']}
              />
            </PieChart>
          </ResponsiveContainer>
          <div {...stylex.props(styles.caption)}>
            按累计下单次数分层（近30天下单用户去重）
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12}>
              <Card>
                <Statistic
                  title="下单用户数"
                  value={orderingUsers}
                  suffix="人"
                />
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card>
                <Statistic
                  title="下单用户占比"
                  value={orderingUserRatio}
                  suffix="%"
                  precision={2}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card>
                <Statistic
                  title="复购率"
                  value={repurchaseRate}
                  suffix="%"
                  precision={2}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12}>
              <Card>
                <Statistic
                  title="总用户数"
                  value={totalUsers}
                  suffix="人"
                />
              </Card>
            </Col>
          </Row>
        </Col>
      </Row>
    </Spin>
  </Card>
);

export default OrderUserDistributionCard;
