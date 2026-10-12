import React from 'react';
import { Avatar, Card, Col, Descriptions, Row, Tag, Typography } from '@aquarush/admin-ui';
import { UserOutlined } from '@aquarush/admin-ui/icons';
import { avatarFromName } from '@aquarush/common/utils';
import * as stylex from '@stylexjs/stylex';
import { ROLE_MAPPINGS } from '../../../components/types';
import type { DeliveryRecord } from '../types';
import type { AdminActionKind } from '../AdminAccountDetail';

const { Paragraph, Text } = Typography;

const styles = stylex.create({
  basicCard: {
    marginBottom: '24px',
  },
  avatar: {
    marginBottom: '16px',
  },
  onlineLine: {
    fontSize: '12px',
    marginBottom: '0px',
  },
  onlineTag: {
    marginLeft: '4px',
  },
  permRow: {
    marginTop: '16px',
  },
});

export interface BasicInfoCardProps {
  record: DeliveryRecord;
  /** 可用操作判定(由页面编排按权限矩阵传入) */
  can: (kind: AdminActionKind) => boolean;
}

/** 基本信息卡(含权限信息),由 AdminAccountDetail.tsx 拆分 */
const BasicInfoCard: React.FC<BasicInfoCardProps> = ({ record, can }) => {
  const roleConfig = ROLE_MAPPINGS[record.role as keyof typeof ROLE_MAPPINGS];

  /** 权限信息(由 role 推导,与 01-overview.md §角色权限矩阵一致,纯展示) */
  const renderPermissionInfo = () => {
    const isSuperAdmin = record.role === 'SUPER_ADMIN';
    const permissionScope = isSuperAdmin
      ? '全权限(账号/用户/商品/订单/统计/营销/运营/日志等)'
      : '配送能力账号范围(账号/送水员相关,受限权限矩阵)';
    return (
      <Descriptions title="权限信息" column={2} size="small">
        <Descriptions.Item label="权限范围">{permissionScope}</Descriptions.Item>
        <Descriptions.Item label="兼送水权限">
          {isSuperAdmin || record.role === 'ADMIN' ? (
            <Tag color="blue">DELIVERY_TASK(可参与配送,F-USR-023)</Tag>
          ) : (
            <Text type="secondary">不参与配送</Text>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="可用操作">
          {[
            can('edit') ? '编辑' : null,
            can('toggleStatus') ? '停用/启用' : null,
            can('resetPassword') ? '重置密码' : null,
            can('delete') ? '删除' : null,
          ]
            .filter(Boolean)
            .join(' / ') || '—'}
        </Descriptions.Item>
        <Descriptions.Item label="删除/重置密码">
          {isSuperAdmin ? (
            <Text>仅超管可用(不能删自己、不能删最后一个超管)</Text>
          ) : (
            <Text type="secondary">仅超管可用(R-AUTH-04)</Text>
          )}
        </Descriptions.Item>
      </Descriptions>
    );
  };

  return (
    <Card {...stylex.props(styles.basicCard)}>
      <Row gutter={24}>
        <Col span={4}>
          <Avatar
            size={120}
            src={record.avatarUrl || avatarFromName(record.username || record.name || '账号')}
            icon={<UserOutlined />}
            {...stylex.props(styles.avatar)}
          />
        </Col>
        <Col span={20}>
          <Descriptions title="基本信息" column={3}>
            <Descriptions.Item label="姓名">
              {record.name || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="用户名">
              {record.username || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="手机号">
              {record.phone || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="角色">
              <Tag color={roleConfig.color}>{roleConfig.text}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="账号状态">
              {record.accountStatus === 'DISABLED' ? (
                <Tag color="red">已停用</Tag>
              ) : (
                <Tag color="green">正常</Tag>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="最后登录时间">
              {record.lastLoginAt
                ? new Date(record.lastLoginAt).toLocaleString('zh-CN')
                : '从未登录'}
            </Descriptions.Item>
            {record.createdAt && (
              <Descriptions.Item label="创建时间">
                {new Date(record.createdAt).toLocaleString('zh-CN')}
              </Descriptions.Item>
            )}
            {record.updatedAt && (
              <Descriptions.Item label="更新时间">
                {new Date(record.updatedAt).toLocaleString('zh-CN')}
              </Descriptions.Item>
            )}
          </Descriptions>
          <Paragraph type="secondary" {...stylex.props(styles.onlineLine)}>
            在线状态:
            <Tag color={record.onlineStatus === 'ONLINE' ? 'success' : 'default'} {...stylex.props(styles.onlineTag)}>
              {record.onlineStatus === 'ONLINE' ? '在线' : '离线'}
            </Tag>
          </Paragraph>
        </Col>
      </Row>
      <Row {...stylex.props(styles.permRow)}>
        <Col span={24}>{renderPermissionInfo()}</Col>
      </Row>
    </Card>
  );
};

export default BasicInfoCard;
