import React from 'react';
import { Avatar, Button, Card, Col, Descriptions, Row, Tabs, Tag } from 'antd';
import { UserOutlined, PhoneOutlined, EnvironmentOutlined, CarryOutOutlined } from '@ant-design/icons';
import { avatarFromName } from '@aquarush/common/utils';
import type { DeliveryRecord } from './types';
import DeliveryStatsCards from './DeliveryStatsCards';
import OrderRecordsSection from './OrderRecordsSection';
import TrajectorySection from './sections/TrajectorySection';
import DailyCollectionSection from './sections/DailyCollectionSection';
import WorkerReviewSection from './sections/WorkerReviewSection';

interface WorkerAccountDetailProps {
  record: DeliveryRecord;
  onBack: () => void;
}

/**
 * 送水员视图(role=DELIVERY_WORKER,PRD/07-user.md §F-USR-021):
 * Tabs 布局:基本信息(含当前任务数)/ 统计 / 配送记录(「订单状态」筛选参数 + 单表格)/
 * 配送轨迹 / 每日收款 / 评价记录。
 */
const WorkerAccountDetail: React.FC<WorkerAccountDetailProps> = ({ record, onBack }) => {
  return (
    <div>
      <div style={{ marginBottom: '12px' }}>
        <Button onClick={onBack}>← 返回账号列表</Button>
      </div>

      <Tabs
        defaultActiveKey="basic"
        items={[
          {
            key: 'basic',
            label: '基本信息',
            children: (
              <Card>
                <Row gutter={24}>
                  <Col span={4}>
                    <Avatar
                      size={120}
                      src={record.avatarUrl || avatarFromName(record.name || '送水员')}
                      icon={<UserOutlined />}
                      style={{ marginBottom: '16px' }}
                    />
                  </Col>
                  <Col span={20}>
                    <Descriptions title="基本信息" column={3}>
                      <Descriptions.Item label="姓名">
                        {record.name || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="手机号">
                        <PhoneOutlined /> {record.phone || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="登录账号">
                        {`delivery_${record.id}`}
                      </Descriptions.Item>
                      <Descriptions.Item label="在线状态">
                        <Tag color={record.onlineStatus === 'ONLINE' ? 'success' : 'default'}>
                          {record.onlineStatus === 'ONLINE' ? '在线' : '离线'}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="账号状态">
                        {record.accountStatus === 'DISABLED' ? (
                          <Tag color="red">已停用</Tag>
                        ) : (
                          <Tag color="green">正常</Tag>
                        )}
                      </Descriptions.Item>
                      <Descriptions.Item label="当前任务数">
                        <CarryOutOutlined />{' '}
                        {record.currentTaskCount != null ? `${record.currentTaskCount} 单待配送` : '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="当前坐标">
                        <EnvironmentOutlined /> {record.coordinates || '未设置'}
                      </Descriptions.Item>
                      <Descriptions.Item label="当前位置">
                        {record.currentLocation || '未更新'}
                      </Descriptions.Item>
                    </Descriptions>
                  </Col>
                </Row>
              </Card>
            ),
          },
          {
            key: 'stats',
            label: '统计',
            children: (
              <DeliveryStatsCards
                totalOrders={record.totalOrders}
                completedOrders={record.completedOrders}
                averageRating={record.averageRating}
                earningCents={record.earningCents}
              />
            ),
          },
          {
            key: 'records',
            label: '配送记录',
            children: <OrderRecordsSection deliveryWorkerId={record.id} />,
          },
          {
            key: 'trajectory',
            label: '配送轨迹',
            children: <TrajectorySection workerId={record.id} />,
          },
          {
            key: 'collection',
            label: '每日收款',
            children: <DailyCollectionSection deliveryWorkerId={record.id} />,
          },
          {
            key: 'reviews',
            label: '评价记录',
            children: <WorkerReviewSection deliveryWorkerId={record.id} />,
          },
        ]}
      />
    </div>
  );
};

export default WorkerAccountDetail;
