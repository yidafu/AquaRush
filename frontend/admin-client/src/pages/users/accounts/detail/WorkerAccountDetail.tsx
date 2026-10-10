import React from 'react';
import { Avatar, Button, Card, Col, Descriptions, Row, Tabs, Tag, Typography } from 'antd';
import { UserOutlined, PhoneOutlined, EnvironmentOutlined, CarryOutOutlined } from '@ant-design/icons';
import { avatarFromName } from '@aquarush/common/utils';
import type { DeliveryRecord } from './types';
import DeliveryStatsCards from './DeliveryStatsCards';
import OrderRecordsSection from './OrderRecordsSection';
import TrajectorySection from './sections/TrajectorySection';
import DailyCollectionSection from './sections/DailyCollectionSection';
import WorkerReviewSection from './sections/WorkerReviewSection';

const { Paragraph } = Typography;

interface WorkerAccountDetailProps {
  record: DeliveryRecord;
  onBack: () => void;
}

/**
 * 送水员视图(role=DELIVERY_WORKER,spec/prd/07-user.md §F-USR-021):
 * 纵向布局:基本信息(含当前任务数)/ 配送统计 固定排在页面顶部,
 * 下方 Tabs:配送记录(「订单状态」筛选参数 + 单表格)/ 配送轨迹 / 每日收款 / 评价记录。
 */
const WorkerAccountDetail: React.FC<WorkerAccountDetailProps> = ({ record, onBack }) => {
  return (
    <div>
      <div style={{ marginBottom: '12px' }}>
        <Button onClick={onBack}>← 返回账号列表</Button>
      </div>

      {/* 基本信息(纵向排列在页面顶部,不在 tabs 内) */}
      <Card style={{ marginBottom: 24 }}>
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
            <Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 0 }}>
              在线状态:
              <Tag color={record.onlineStatus === 'ONLINE' ? 'success' : 'default'} style={{ marginLeft: 4 }}>
                {record.onlineStatus === 'ONLINE' ? '在线' : '离线'}
              </Tag>
            </Paragraph>
          </Col>
        </Row>
      </Card>

      {/* 配送统计(纵向排列在页面顶部,不在 tabs 内) */}
      <DeliveryStatsCards
        totalOrders={record.totalOrders}
        completedOrders={record.completedOrders}
        averageRating={record.averageRating}
        earningCents={record.earningCents}
      />

      <Tabs
        defaultActiveKey="records"
        items={[
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
