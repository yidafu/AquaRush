import React from 'react';
import { Modal, Descriptions, Tag, Avatar, Progress, Statistic, Row, Col } from 'antd';
import { UserOutlined, EnvironmentOutlined, PhoneOutlined } from '@ant-design/icons';
import type { DeliveryWorker, Address, ModalProps } from './types';
import { WORKER_STATUS_MAPPINGS } from './types';
import AddressList from './AddressList';
import { centsToYuan } from '../../../utils/money';

interface DeliveryWorkerDetailModalProps extends ModalProps {
  deliveryWorker: DeliveryWorker | null;
  addresses: Address[];
}

const DeliveryWorkerDetailModal: React.FC<DeliveryWorkerDetailModalProps> = ({
  open,
  onClose,
  loading = false,
  deliveryWorker,
  addresses = [],
}) => {
  return (
    <Modal
      title="送水员详情"
      open={open}
      onCancel={onClose}
      footer={null}
      width={1000}
      destroyOnClose
    >
      {deliveryWorker ? (
        <div style={{ padding: '16px 0' }}>
          <div style={{ textAlign: 'center', marginBottom: 32 }}>
            <Avatar
              size={80}
              src={deliveryWorker.avatarUrl}
              icon={<UserOutlined />}
              style={{ marginBottom: 16, backgroundColor: '#87d068' }}
            />
            <h2 style={{ margin: 0, fontSize: 24 }}>{deliveryWorker.name}</h2>
            <div style={{ marginTop: 8 }}>
              <Tag color={WORKER_STATUS_MAPPINGS[deliveryWorker.status].color}>
                {WORKER_STATUS_MAPPINGS[deliveryWorker.status].text}
              </Tag>
              {!deliveryWorker.isAvailable && (
                <Tag color="default" style={{ marginLeft: 8 }}>
                  不可接单
                </Tag>
              )}
            </div>
          </div>

          <Row gutter={[16, 16]} style={{ marginBottom: 32 }}>
            <Col span={6}>
              <Statistic
                title="总订单数"
                value={deliveryWorker.totalOrders}
                suffix="单"
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="已完成订单"
                value={deliveryWorker.completedOrders}
                suffix="单"
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="完成率"
                value={deliveryWorker.totalOrders > 0
                  ? ((deliveryWorker.completedOrders / deliveryWorker.totalOrders) * 100).toFixed(1)
                  : 0}
                suffix="%"
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="总收入"
                value={centsToYuan(deliveryWorker.earning || 0)}
                precision={2}
                prefix="¥"
              />
            </Col>
          </Row>

          <Descriptions
            column={2}
            bordered
            size="middle"
            labelStyle={{ width: 140, fontWeight: 'bold' }}
            style={{ marginBottom: 24 }}
          >
            <Descriptions.Item label="送水员ID">
              {deliveryWorker.id}
            </Descriptions.Item>
            <Descriptions.Item label="用户ID">
              {deliveryWorker.userId}
            </Descriptions.Item>
            <Descriptions.Item label="微信OpenID">
              <span title={deliveryWorker.wechatOpenId}>
                {deliveryWorker.wechatOpenId
                  ? `${deliveryWorker.wechatOpenId.substring(0, 12)}...`
                  : '-'}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="手机号">
              <span>
                <PhoneOutlined style={{ marginRight: 8 }} />
                {deliveryWorker.phone}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="评分">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Progress
                  percent={Math.min((deliveryWorker.averageRating || deliveryWorker.rating || 0) * 20, 100)}
                  size="small"
                  showInfo={false}
                  style={{ flex: 1, maxWidth: 100 }}
                  strokeColor={(deliveryWorker.averageRating || deliveryWorker.rating || 0) >= 4 ? '#52c41a' : '#faad14'}
                />
                <span>
                  {(deliveryWorker.averageRating || deliveryWorker.rating || 0).toFixed(1)} 分
                </span>
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="当前位置">
              {deliveryWorker.currentLocation ? (
                <span>
                  <EnvironmentOutlined style={{ marginRight: 8 }} />
                  {deliveryWorker.currentLocation}
                </span>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="坐标信息">
              {deliveryWorker.coordinates ? (
                <span title={deliveryWorker.coordinates}>
                  {deliveryWorker.coordinates.substring(0, 20)}...
                </span>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {new Date(deliveryWorker.createdAt).toLocaleString('zh-CN')}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {new Date(deliveryWorker.updatedAt).toLocaleString('zh-CN')}
            </Descriptions.Item>
          </Descriptions>

          <AddressList addresses={addresses} loading={loading} />
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <p>暂无数据</p>
        </div>
      )}
    </Modal>
  );
};

export default DeliveryWorkerDetailModal;