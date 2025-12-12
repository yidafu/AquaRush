import React from 'react';
import { Modal, Descriptions, Tag, Avatar } from 'antd';
import { UserOutlined, WechatOutlined } from '@ant-design/icons';
import type { User, Address, ModalProps } from './types';
import { STATUS_MAPPINGS } from './types';
import AddressList from './AddressList';

interface UserDetailModalProps extends ModalProps {
  user: User | null;
  addresses: Address[];
}

const UserDetailModal: React.FC<UserDetailModalProps> = ({
  open,
  onClose,
  loading = false,
  user,
  addresses = [],
}) => {
  const getRoleDisplay = (role: string) => {
    const roleMap = {
      USER: { color: 'blue', text: '用户' },
      ADMIN: { color: 'red', text: '管理员' },
      WORKER: { color: 'green', text: '配送员' },
      NONE: { color: 'default', text: '无' },
    };
    return roleMap[role as keyof typeof roleMap] || { color: 'default', text: role };
  };

  return (
    <Modal
      title="用户详情"
      open={open}
      onCancel={onClose}
      footer={null}
      width={800}
      destroyOnClose
    >
      {user ? (
        <div style={{ padding: '16px 0' }}>
          <div style={{ textAlign: 'center', marginBottom: 32 }}>
            <Avatar
              size={80}
              src={user.avatarUrl}
              icon={<UserOutlined />}
              style={{ marginBottom: 16, backgroundColor: '#1890ff' }}
            />
            <h2 style={{ margin: 0, fontSize: 24 }}>
              {user.nickname || '未设置昵称'}
            </h2>
            <div style={{ marginTop: 8 }}>
              <Tag color={STATUS_MAPPINGS[user.status].color}>
                {STATUS_MAPPINGS[user.status].text}
              </Tag>
              <Tag
                color={getRoleDisplay(user.role).color}
                style={{ marginLeft: 8 }}
              >
                {getRoleDisplay(user.role).text}
              </Tag>
            </div>
          </div>

          <Descriptions
            column={2}
            bordered
            size="middle"
            labelStyle={{ width: 140, fontWeight: 'bold' }}
            style={{ marginBottom: 24 }}
          >
            <Descriptions.Item label="用户ID">
              {user.id}
            </Descriptions.Item>
            <Descriptions.Item label="昵称">
              {user.nickname || '未设置昵称'}
            </Descriptions.Item>
            <Descriptions.Item label="手机号">
              {user.phone || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="微信OpenID">
              <span title={user.wechatOpenId}>
                <WechatOutlined style={{ marginRight: 8, color: '#07c160' }} />
                {user.wechatOpenId
                  ? `${user.wechatOpenId.substring(0, 12)}...`
                  : '-'}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={STATUS_MAPPINGS[user.status].color}>
                {STATUS_MAPPINGS[user.status].text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="角色">
              <Tag color={getRoleDisplay(user.role).color}>
                {getRoleDisplay(user.role).text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="头像URL">
              {user.avatarUrl ? (
                <a href={user.avatarUrl} target="_blank" rel="noopener noreferrer">
                  查看头像
                </a>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="注册时间">
              {new Date(user.createdAt).toLocaleString('zh-CN')}
            </Descriptions.Item>
            <Descriptions.Item label="最后更新">
              {new Date(user.updatedAt).toLocaleString('zh-CN')}
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

export default UserDetailModal;