import React from 'react';
import { Card, Row, Col, Statistic } from 'antd';
import { ShoppingCartOutlined, DollarOutlined, UserOutlined } from '@ant-design/icons';

const Dashboard: React.FC = () => {
  return (
    <div>
      <h1 className="text-3xl font-bold text-blue-500">仪表盘</h1>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="今日订单"
              value={0}
              prefix={<ShoppingCartOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="今日营收"
              value={0}
              prefix={<DollarOutlined />}
              suffix="元"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="用户总数"
              value={0}
              prefix={<UserOutlined />}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
