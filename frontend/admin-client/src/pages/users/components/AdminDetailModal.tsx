import React from 'react';
import { Modal, Descriptions, Tag, Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { Admin, ModalProps } from './types';
import { ROLE_MAPPINGS } from './types';

interface AdminDetailModalProps extends ModalProps {
  admin: Admin | null;
}

const AdminDetailModal: React.FC<AdminDetailModalProps> = ({
  open,
  onClose,
  admin,
}) => {
  return (
    <Modal
      title="管理员详情"
      open={open}
      onCancel={onClose}
      footer={null}
      width={800}
      destroyOnClose
    >
      {admin ? (
        <div style={{ padding: '16px 0' }}>
          <div style={{ textAlign: 'center', marginBottom: 32 }}>
            <Avatar
              size={80}
              icon={<UserOutlined />}
              style={{ marginBottom: 16, backgroundColor: '#1890ff' }}
            />
            <h2 style={{ margin: 0, fontSize: 24 }}>{admin.realName || admin.username}</h2>
            <div style={{ marginTop: 8 }}>
              <Tag color={ROLE_MAPPINGS[admin.role].color}>
                {ROLE_MAPPINGS[admin.role].text}
              </Tag>
            </div>
          </div>

          <Descriptions
            column={2}
            bordered
            size="middle"
            labelStyle={{ width: 120, fontWeight: 'bold' }}
          >
            <Descriptions.Item label="管理员ID">
              {admin.id}
            </Descriptions.Item>
            <Descriptions.Item label="用户名">
              {admin.username}
            </Descriptions.Item>
            <Descriptions.Item label="真实姓名">
              {admin.realName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="手机号">
              {admin.phone || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="角色">
              <Tag color={ROLE_MAPPINGS[admin.role].color}>
                {ROLE_MAPPINGS[admin.role].text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="最后登录时间">
              {admin.lastLoginAt
                ? new Date(admin.lastLoginAt).toLocaleString('zh-CN')
                : '从未登录'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {new Date(admin.createdAt).toLocaleString('zh-CN')}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {new Date(admin.updatedAt).toLocaleString('zh-CN')}
            </Descriptions.Item>
          </Descriptions>
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <p>暂无数据</p>
        </div>
      )}
    </Modal>
  );
};

export default AdminDetailModal;