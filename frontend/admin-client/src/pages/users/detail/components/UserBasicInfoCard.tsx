import React from 'react';
import { Card, Row, Col, Avatar, Descriptions, Tag } from 'antd';
import {
  UserOutlined,
  PhoneOutlined,
  TeamOutlined,
  DollarOutlined,
  MailOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import type { User, UserRole } from '@aquarush/common';
import { UserRole as UserRoleEnum } from '@aquarush/common';
import { displayCents } from '@aquarush/common';
import { STATUS_MAPPINGS } from '../../components/types';

const ROLE_LABELS: Record<UserRole, string> = {
  [UserRoleEnum.NONE]: '未分配',
  [UserRoleEnum.USER]: '普通用户',
  [UserRoleEnum.ADMIN]: '管理员',
  [UserRoleEnum.WORKER]: '送水员',
};

export interface UserBasicInfoCardProps {
  user: User;
  /** 最近下单时间(已格式化的展示文案) */
  lastOrderTime: string;
}

/** 用户基本信息卡(头像 + Descriptions,users/detail/index.tsx 拆分) */
const UserBasicInfoCard: React.FC<UserBasicInfoCardProps> = ({ user, lastOrderTime }) => {
  const statusConfig = STATUS_MAPPINGS[user.status as keyof typeof STATUS_MAPPINGS];

  return (
    <Card style={{ marginBottom: '24px' }}>
      <Row gutter={24}>
        <Col span={4}>
          <Avatar
            size={120}
            src={user.avatarUrl}
            icon={<UserOutlined />}
            style={{ marginBottom: '16px' }}
          />
        </Col>
        <Col span={20}>
          <Descriptions title="基本信息" column={3}>
            <Descriptions.Item label="用户ID">
              {user.id || '未绑定'}
            </Descriptions.Item>
            <Descriptions.Item label="昵称">
              {user.nickname || '未设置'}
            </Descriptions.Item>
            <Descriptions.Item label="手机号">
              <PhoneOutlined /> {user.phone || '未绑定'}
            </Descriptions.Item>
            <Descriptions.Item label="微信OpenID">
              {user.wechatOpenId}
            </Descriptions.Item>
            <Descriptions.Item label="用户角色">
              <Tag color="geekblue">{ROLE_LABELS[user.role] ?? user.role}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="账户状态">
              <Tag color={statusConfig?.color ?? 'default'}>
                {statusConfig?.text ?? user.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="所属客户分组">
              <TeamOutlined /> {user.customerGroupName || '未分组'}
            </Descriptions.Item>
            <Descriptions.Item label="总消费金额">
              <DollarOutlined /> {displayCents(user.totalSpentCents)}
            </Descriptions.Item>
            <Descriptions.Item label="邮箱">
              <MailOutlined /> {user.email || '未设置'}
            </Descriptions.Item>
            <Descriptions.Item label="最近下单时间">
              <CalendarOutlined /> {lastOrderTime}
            </Descriptions.Item>
            <Descriptions.Item label="注册时间">
              <CalendarOutlined /> {new Date(user.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="最后更新">
              {new Date(user.updatedAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
        </Col>
      </Row>
    </Card>
  );
};

export default UserBasicInfoCard;
