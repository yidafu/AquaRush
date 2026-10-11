import React from 'react';
import {
  Avatar,
  Button,
  Card,
  Col,
  Descriptions,
  Row,
  Space,
  Tabs,
  Tag,
  Typography,
} from '@/ui';
import {
  DeleteOutlined,
  EditOutlined,
  EnvironmentOutlined,
  KeyOutlined,
  LockOutlined,
  UnlockOutlined,
  UserOutlined,
} from '@/ui/icons';
import { avatarFromName } from '@aquarush/common/utils';
import * as stylex from '@stylexjs/stylex';
import { ROLE_MAPPINGS } from '../../components/types';
import type { AccountDetailActions, DeliveryRecord } from './types';
import DeliveryStatsCards from './DeliveryStatsCards';
import OrderRecordsSection from './OrderRecordsSection';
import TrajectorySection from './sections/TrajectorySection';
import DailyCollectionSection from './sections/DailyCollectionSection';
import WorkerReviewSection from './sections/WorkerReviewSection';

const { Paragraph, Text } = Typography;

const styles = stylex.create({
  backWrap: {
    marginBottom: '12px',
  },
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

export type AdminActionKind = 'edit' | 'resetPassword' | 'toggleStatus' | 'delete';

interface AdminAccountDetailProps {
  record: DeliveryRecord;
  onBack: () => void;
  actions: AccountDetailActions;
  /** 可用操作(默认全可用;普通管理员查看兼送水管理员时删除/重置密码不可用,由页面编排按权限矩阵传入) */
  availableActions?: Partial<Record<AdminActionKind, boolean>>;
}

/**
 * 管理账号视图(role=ADMIN/SUPER_ADMIN,spec/prd/07-user.md §F-USR-020):
 * 纵向布局:基本信息(含权限信息)/ 配送统计 固定排在页面顶部,
 * 下方 Tabs:账号安全 / 配送能力(兼送水,F-USR-023)/ 配送记录 / 配送轨迹 /
 * 每日收款 / 评价记录 / 操作。
 * 管理员参与配送,配送记录/轨迹/日报/评价按账号 id 查询。
 */
const AdminAccountDetail: React.FC<AdminAccountDetailProps> = ({
  record,
  onBack,
  actions,
  availableActions = {},
}) => {
  const can = (kind: AdminActionKind) => availableActions[kind] !== false;

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
    <div>
      <div {...stylex.props(styles.backWrap)}>
        <Button onClick={onBack}>← 返回账号列表</Button>
      </div>

      {/* 基本信息(纵向排列在页面顶部,不在 tabs 内) */}
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

      {/* 配送统计(纵向排列在页面顶部,不在 tabs 内) */}
      <DeliveryStatsCards
        totalOrders={record.totalOrders}
        completedOrders={record.completedOrders}
        averageRating={record.averageRating}
        earningCents={record.earningCents}
      />

      <Tabs
        defaultActiveKey="security"
        items={[
          {
            key: 'security',
            label: '账号安全',
            children: (
              <Card title="账号安全">
                <Descriptions column={3}>
                  <Descriptions.Item label="登录账号">
                    {record.username || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="账号状态">
                    {record.accountStatus === 'DISABLED' ? (
                      <Tag color="red">已停用</Tag>
                    ) : (
                      <Tag color="green">正常</Tag>
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="密码管理">
                    <Space size="small">
                      {can('resetPassword') && (
                        <Button
                          type="link"
                          size="small"
                          icon={<KeyOutlined />}
                          onClick={actions.onResetPassword}
                        >
                          重置密码
                        </Button>
                      )}
                      {can('toggleStatus') && (
                        <Button
                          type="link"
                          size="small"
                          icon={record.accountStatus === 'DISABLED' ? <UnlockOutlined /> : <LockOutlined />}
                          onClick={actions.onToggleStatus}
                        >
                          {record.accountStatus === 'DISABLED' ? '启用' : '停用'}
                        </Button>
                      )}
                    </Space>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'delivery',
            label: '配送能力',
            children: (
              // 配送能力卡(兼送水:ADMIN/SUPER_ADMIN 均可参与配送,F-USR-023);
              // 配送统计已移至页面顶部纵向排列
              <Card title="配送能力">
                <Descriptions column={3}>
                  <Descriptions.Item label="在线状态">
                    <Tag color={record.onlineStatus === 'ONLINE' ? 'success' : 'default'}>
                      {record.onlineStatus === 'ONLINE' ? '在线' : '离线'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="接单权限">
                    {record.isAvailable ? '可接单' : '不可接单'}
                  </Descriptions.Item>
                  <Descriptions.Item label="当前坐标">
                    <EnvironmentOutlined /> {record.coordinates || '未设置'}
                  </Descriptions.Item>
                  <Descriptions.Item label="当前位置">
                    {record.currentLocation || '未更新'}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
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
          {
            key: 'operations',
            label: '操作',
            children: (
              <Card title="操作">
                <Space size="small">
                  {can('edit') && (
                    <Button icon={<EditOutlined />} onClick={actions.onEdit}>
                      编辑
                    </Button>
                  )}
                  {can('resetPassword') && (
                    <Button icon={<KeyOutlined />} onClick={actions.onResetPassword}>
                      重置密码
                    </Button>
                  )}
                  {can('toggleStatus') && (
                    <Button
                      icon={record.accountStatus === 'DISABLED' ? <UnlockOutlined /> : <LockOutlined />}
                      onClick={actions.onToggleStatus}
                    >
                      {record.accountStatus === 'DISABLED' ? '启用' : '停用'}
                    </Button>
                  )}
                  {can('delete') && (
                    <Button danger icon={<DeleteOutlined />} onClick={actions.onDelete}>
                      删除
                    </Button>
                  )}
                </Space>
              </Card>
            ),
          },
        ]}
      />
    </div>
  );
};

export default AdminAccountDetail;
