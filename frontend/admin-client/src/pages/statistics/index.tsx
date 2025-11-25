import React from 'react';
import { Card, DatePicker, Row, Col } from 'antd';

const { RangePicker } = DatePicker;

const Statistics: React.FC = () => {
  return (
    <div>
      <h1>营收统计</h1>
      <div style={{ marginTop: 16 }}>
        <RangePicker />
      </div>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={12}>
          <Card title="订单趋势">
            {/* TODO: 添加图表 */}
            <p>图表占位</p>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="营收趋势">
            {/* TODO: 添加图表 */}
            <p>图表占位</p>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Statistics;
