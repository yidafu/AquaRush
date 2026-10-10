import React from 'react';
import { Timeline, Tag, Typography, Empty, Spin } from 'antd';
import {
  PlusOutlined,
  DollarOutlined,
  CloseCircleOutlined,
  UserOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SafetyOutlined,
  SwapOutlined,
  LoginOutlined,
  LogoutOutlined,
  UserAddOutlined,
  EditOutlined,
  StopOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { formatDateTime } from '@aquarush/common/utils';

const { Text } = Typography;

export interface BusinessLogItem {
  id: string;
  correlationId?: string;
  level: 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  loggerName?: string;
  operation?: string;
  message?: string;
  stackTrace?: string;
  userId?: number;
  username?: string;
  createdAt: string;
}

// 日志级别颜色(TRACE default / DEBUG green / INFO blue / WARN orange / ERROR red)
const LOG_LEVEL_COLOR: Record<string, string> = {
  TRACE: 'default',
  DEBUG: 'green',
  INFO: 'blue',
  WARN: 'orange',
  ERROR: 'red',
};

// 操作类型到图标/颜色/中文标签的映射(域 → 操作;未命中回退原始值)
export const OPERATION_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  // AUTH 认证
  USER_LOGIN: { icon: <LoginOutlined />, color: 'blue', label: '用户登录' },
  ADMIN_LOGIN: { icon: <LoginOutlined />, color: 'purple', label: '管理员登录' },
  LOGIN_FAILED: { icon: <CloseCircleOutlined />, color: 'red', label: '登录失败' },
  LOGOUT: { icon: <LogoutOutlined />, color: 'default', label: '登出' },
  // ORDER 订单
  ORDER_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '订单创建' },
  ORDER_PAID: { icon: <DollarOutlined />, color: 'green', label: '订单支付' },
  ORDER_CANCELLED: { icon: <CloseCircleOutlined />, color: 'red', label: '订单取消' },
  ORDER_COMPLETED: { icon: <CheckCircleOutlined />, color: 'green', label: '订单完成' },
  ORDER_CONFIRMED: { icon: <CheckCircleOutlined />, color: 'green', label: '用户确认收货(历史)' },
  ORDER_AUTO_CONFIRMED: { icon: <CheckCircleOutlined />, color: 'green', label: '系统自动确认收货(历史)' },
  ORDER_STATUS_CHANGED: { icon: <SwapOutlined />, color: 'orange', label: '订单状态变更' },
  ORDER_REFUND: { icon: <SafetyOutlined />, color: 'purple', label: '订单退款' },
  // PAYMENT 支付
  PAYMENT_CREATED: { icon: <DollarOutlined />, color: 'cyan', label: '支付发起' },
  PAYMENT_COMPLETED: { icon: <CheckCircleOutlined />, color: 'green', label: '支付完成' },
  PAYMENT_FAILED: { icon: <CloseCircleOutlined />, color: 'red', label: '支付失败' },
  PAYMENT_REFUNDED: { icon: <SafetyOutlined />, color: 'purple', label: '退款完成' },
  PAYMENT_TIMEOUT: { icon: <ClockCircleOutlined />, color: 'default', label: '支付超时' },
  // DELIVERY 配送
  DELIVERY_ASSIGNED: { icon: <UserOutlined />, color: 'cyan', label: '配送员分配' },
  DELIVERY_ACCEPTED: { icon: <CheckCircleOutlined />, color: 'green', label: '配送员接单' },
  DELIVERY_STARTED: { icon: <CarOutlined />, color: 'orange', label: '开始配送' },
  DELIVERY_COMPLETED: { icon: <CheckCircleOutlined />, color: 'green', label: '配送完成' },
  DELIVERY_CANCELLED: { icon: <CloseCircleOutlined />, color: 'red', label: '配送取消' },
  DELIVERY_WORKER_CREATED: { icon: <UserAddOutlined />, color: 'blue', label: '配送员创建' },
  DELIVERY_WORKER_STATUS_CHANGED: { icon: <SwapOutlined />, color: 'orange', label: '配送员状态变更' },
  DELIVERY_AREA_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '配送区域创建' },
  DELIVERY_AREA_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '配送区域更新' },
  // USER 用户
  USER_CREATED: { icon: <UserAddOutlined />, color: 'blue', label: '用户创建' },
  USER_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '用户资料更新' },
  USER_STATUS_CHANGED: { icon: <StopOutlined />, color: 'red', label: '用户状态变更' },
  USER_ROLE_CHANGED: { icon: <TeamOutlined />, color: 'purple', label: '角色变更' },
  ADMIN_CREATED: { icon: <UserAddOutlined />, color: 'blue', label: '管理员创建' },
  ADMIN_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '管理员更新' },
  ADMIN_DELETED: { icon: <StopOutlined />, color: 'red', label: '管理员删除' },
  ADDRESS_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '地址新增' },
  ADDRESS_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '地址更新' },
  ADDRESS_DELETED: { icon: <StopOutlined />, color: 'red', label: '地址删除' },
  ADDRESS_DEFAULT_CHANGED: { icon: <SwapOutlined />, color: 'purple', label: '默认地址变更' },
  // PRODUCT 产品
  PRODUCT_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '产品创建' },
  PRODUCT_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '产品更新' },
  PRODUCT_DELETED: { icon: <StopOutlined />, color: 'red', label: '产品删除' },
  PRODUCT_STATUS_CHANGED: { icon: <SwapOutlined />, color: 'orange', label: '产品状态变更' },
  // MARKETING 营销
  COUPON_TEMPLATE_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '券模板创建' },
  COUPON_TEMPLATE_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '券模板更新' },
  COUPON_TEMPLATE_DISABLED: { icon: <StopOutlined />, color: 'red', label: '券模板停用' },
  COUPON_ISSUED: { icon: <DollarOutlined />, color: 'green', label: '优惠券发放' },
  CUSTOMER_GROUP_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '客户分组创建' },
  CUSTOMER_GROUP_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '客户分组更新' },
  CUSTOMER_GROUP_DELETED: { icon: <StopOutlined />, color: 'red', label: '客户分组删除' },
  CUSTOMER_GROUP_USER_SET: { icon: <TeamOutlined />, color: 'purple', label: '用户分组设置' },
  FLASH_SALE_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '闪购活动创建' },
  FLASH_SALE_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '闪购活动更新' },
  FLASH_SALE_DISABLED: { icon: <StopOutlined />, color: 'red', label: '闪购活动停用' },
  GIFT_ACTIVITY_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '买赠活动创建' },
  GIFT_ACTIVITY_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '买赠活动更新' },
  GIFT_ACTIVITY_DISABLED: { icon: <StopOutlined />, color: 'red', label: '买赠活动停用' },
  FIRST_ORDER_ACTIVITY_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '首单活动创建' },
  FIRST_ORDER_ACTIVITY_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '首单活动更新' },
  FIRST_ORDER_ACTIVITY_DISABLED: { icon: <StopOutlined />, color: 'red', label: '首单活动停用' },
  PROMOTION_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '促销活动创建' },
  PROMOTION_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '促销活动更新' },
  PROMOTION_DISABLED: { icon: <StopOutlined />, color: 'red', label: '促销活动停用' },
  TICKET_TYPE_CREATED: { icon: <PlusOutlined />, color: 'blue', label: '票种创建' },
  TICKET_TYPE_UPDATED: { icon: <EditOutlined />, color: 'orange', label: '票种更新' },
  TICKET_TYPE_DISABLED: { icon: <StopOutlined />, color: 'red', label: '票种停用' },
  TICKET_PURCHASED: { icon: <DollarOutlined />, color: 'green', label: '购票' },
  GROUP_PRICE_SAVED: { icon: <EditOutlined />, color: 'orange', label: '分组定价保存' },
  // OPS 运营配置
  OPS_BANNER_CREATE: { icon: <PlusOutlined />, color: 'blue', label: '轮播图创建' },
  OPS_BANNER_UPDATE: { icon: <EditOutlined />, color: 'orange', label: '轮播图更新' },
  OPS_BANNER_DELETE: { icon: <StopOutlined />, color: 'red', label: '轮播图删除' },
  OPS_BANNER_REORDER: { icon: <SwapOutlined />, color: 'purple', label: '轮播图排序' },
  OPS_FEEDBACK_PROCESS: { icon: <EditOutlined />, color: 'orange', label: '反馈处理' },
  OPS_NOTICE_CREATE: { icon: <PlusOutlined />, color: 'blue', label: '公告创建' },
  OPS_NOTICE_UPDATE: { icon: <EditOutlined />, color: 'orange', label: '公告更新' },
  OPS_NOTICE_PUBLISH: { icon: <CheckCircleOutlined />, color: 'green', label: '公告发布' },
  OPS_NOTICE_PIN: { icon: <SwapOutlined />, color: 'purple', label: '公告置顶' },
  OPS_NOTICE_UNPIN: { icon: <SwapOutlined />, color: 'default', label: '公告取消置顶' },
  OPS_NOTICE_STOP: { icon: <StopOutlined />, color: 'red', label: '公告停用' },
  OPS_NOTICE_DELETE: { icon: <StopOutlined />, color: 'red', label: '公告删除' },
  OPS_STORE_PROFILE_SAVE: { icon: <EditOutlined />, color: 'orange', label: '店铺资料保存' },
  OPS_DELIVERY_FEE_RULE_SAVE: { icon: <EditOutlined />, color: 'orange', label: '配送费规则保存' },
  SYS_CONFIG_SAVE: { icon: <EditOutlined />, color: 'orange', label: '系统配置保存' },
  SYS_CONFIG_RESTORE: { icon: <SwapOutlined />, color: 'purple', label: '系统配置还原' },
  // RECONCILIATION 对账
  RECON_DAILY_SUBMIT: { icon: <EditOutlined />, color: 'blue', label: '提交每日收款' },
  RECON_DAILY_CONFIRM: { icon: <CheckCircleOutlined />, color: 'green', label: '确认收款记录' },
  RECON_REVIEW: { icon: <SafetyOutlined />, color: 'purple', label: '对账复核' },
  RECON_EXECUTE: { icon: <SwapOutlined />, color: 'orange', label: '执行对账' },
};

// 业务域 → 该域操作类型(筛选下拉联动用)
export const DOMAIN_OPERATIONS: Record<string, string[]> = {
  AUTH: ['USER_LOGIN', 'ADMIN_LOGIN', 'LOGIN_FAILED', 'LOGOUT'],
  ORDER: ['ORDER_CREATED', 'ORDER_PAID', 'ORDER_CANCELLED', 'ORDER_COMPLETED', 'ORDER_STATUS_CHANGED', 'ORDER_REFUND'],
  PAYMENT: ['PAYMENT_CREATED', 'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_REFUNDED', 'PAYMENT_TIMEOUT'],
  DELIVERY: [
    'DELIVERY_ASSIGNED',
    'DELIVERY_ACCEPTED',
    'DELIVERY_STARTED',
    'DELIVERY_COMPLETED',
    'DELIVERY_CANCELLED',
    'DELIVERY_WORKER_CREATED',
    'DELIVERY_WORKER_STATUS_CHANGED',
    'DELIVERY_AREA_CREATED',
    'DELIVERY_AREA_UPDATED',
  ],
  USER: [
    'USER_CREATED',
    'USER_UPDATED',
    'USER_STATUS_CHANGED',
    'USER_ROLE_CHANGED',
    'ADMIN_CREATED',
    'ADMIN_UPDATED',
    'ADMIN_DELETED',
    'ADDRESS_CREATED',
    'ADDRESS_UPDATED',
    'ADDRESS_DELETED',
    'ADDRESS_DEFAULT_CHANGED',
  ],
  PRODUCT: ['PRODUCT_CREATED', 'PRODUCT_UPDATED', 'PRODUCT_DELETED', 'PRODUCT_STATUS_CHANGED'],
  MARKETING: [
    'COUPON_TEMPLATE_CREATED',
    'COUPON_TEMPLATE_UPDATED',
    'COUPON_TEMPLATE_DISABLED',
    'COUPON_ISSUED',
    'CUSTOMER_GROUP_CREATED',
    'CUSTOMER_GROUP_UPDATED',
    'CUSTOMER_GROUP_DELETED',
    'CUSTOMER_GROUP_USER_SET',
    'FLASH_SALE_CREATED',
    'FLASH_SALE_UPDATED',
    'FLASH_SALE_DISABLED',
    'GIFT_ACTIVITY_CREATED',
    'GIFT_ACTIVITY_UPDATED',
    'GIFT_ACTIVITY_DISABLED',
    'FIRST_ORDER_ACTIVITY_CREATED',
    'FIRST_ORDER_ACTIVITY_UPDATED',
    'FIRST_ORDER_ACTIVITY_DISABLED',
    'PROMOTION_CREATED',
    'PROMOTION_UPDATED',
    'PROMOTION_DISABLED',
    'TICKET_TYPE_CREATED',
    'TICKET_TYPE_UPDATED',
    'TICKET_TYPE_DISABLED',
    'TICKET_PURCHASED',
    'GROUP_PRICE_SAVED',
  ],
  OPS: [
    'OPS_BANNER_CREATE',
    'OPS_BANNER_UPDATE',
    'OPS_BANNER_DELETE',
    'OPS_BANNER_REORDER',
    'OPS_FEEDBACK_PROCESS',
    'OPS_NOTICE_CREATE',
    'OPS_NOTICE_UPDATE',
    'OPS_NOTICE_PUBLISH',
    'OPS_NOTICE_PIN',
    'OPS_NOTICE_UNPIN',
    'OPS_NOTICE_STOP',
    'OPS_NOTICE_DELETE',
    'OPS_STORE_PROFILE_SAVE',
    'OPS_DELIVERY_FEE_RULE_SAVE',
    'SYS_CONFIG_SAVE',
    'SYS_CONFIG_RESTORE',
  ],
  RECONCILIATION: ['RECON_DAILY_SUBMIT', 'RECON_DAILY_CONFIRM', 'RECON_REVIEW', 'RECON_EXECUTE'],
};

const getOperationConfig = (operation?: string): { icon: React.ReactNode; color: string; label: string } => {
  if (operation && OPERATION_CONFIG[operation]) {
    return OPERATION_CONFIG[operation];
  }
  // 老数据 operation 为 NULL 时回退解析 message 前缀 "Operation: X"
  const fallback = operation || '';
  return {
    icon: <ClockCircleOutlined />,
    color: 'gray',
    label: fallback,
  };
};

const parseOperationFromMessage = (message?: string): string | undefined => {
  if (!message) return undefined;
  const match = message.match(/Operation:\s*([^,\s]+)/);
  return match?.[1];
};

interface BusinessLogTimelineProps {
  logs: BusinessLogItem[];
  loading?: boolean;
}

/**
 * 业务日志时间线视图(F-LOG-003):以用户为视角展示业务操作记录,恒 createdAt 倒序。
 * 条目含 级别 Tag、操作类型(图标+色+文案)、用户 Tag、消息(省略)、关联ID、本地化时间。
 */
const BusinessLogTimeline: React.FC<BusinessLogTimelineProps> = ({ logs, loading }) => {
  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 0' }}>
        <Spin />
      </div>
    );
  }

  if (logs.length === 0) {
    return <Empty description="暂无操作记录" style={{ padding: '48px 0' }} />;
  }

  return (
    <Timeline
      mode="left"
      items={logs.map((log) => {
        const operation = log.operation || parseOperationFromMessage(log.message);
        const config = getOperationConfig(operation);
        const userLabel = log.username || (log.userId != null ? String(log.userId) : '-');
        return {
          color: config.color,
          dot: config.icon,
          children: (
            <div style={{ paddingBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', flexWrap: 'wrap' }}>
                <Text strong>{config.label}</Text>
                <Tag color={LOG_LEVEL_COLOR[log.level] || 'default'}>{log.level}</Tag>
                <Tag color="blue">{userLabel}</Tag>
              </div>
              {log.message && (
                <Typography.Paragraph
                  type="secondary"
                  ellipsis={{ rows: 2 }}
                  style={{ marginBottom: 4, fontSize: 13 }}
                >
                  {log.message}
                </Typography.Paragraph>
              )}
              {log.correlationId && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  关联ID: {log.correlationId}
                </Text>
              )}
              <div style={{ marginTop: '4px' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {formatDateTime(log.createdAt)}
                </Text>
              </div>
            </div>
          ),
        };
      })}
    />
  );
};

export default BusinessLogTimeline;
