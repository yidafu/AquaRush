import React from 'react';
import { Card, Tag, Text } from '@aquarush/admin-ui';
import {
  PlusOutlined,
  DollarOutlined,
  CloseCircleOutlined,
  UserOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SafetyOutlined,
} from '@aquarush/admin-ui/icons';
import JsonViewer from '../JsonViewer';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// antd Timeline 预设色 → 真实色值(与 @aquarush/admin-ui Tag 预设文字色一致);
// tokens 无对应值,字面量保留
const TIMELINE_COLORS: Record<string, string> = {
  blue: '#1677FF',
  green: '#52C41A',
  red: '#FF4D4F',
  orange: '#FA8C16',
  cyan: '#13C2C2',
  purple: '#722ED1',
  default: 'rgba(0, 0, 0, 0.35)',
  gray: 'rgba(0, 0, 0, 0.35)',
};

const styles = stylex.create({
  item: {
    paddingBottom: '8px',
  },
  itemHeader: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    marginBottom: '4px',
  },
  metaRow: {
    marginTop: '4px',
  },
  time: {
    fontSize: '12px',
  },
  // —— 垂直时间线(替代 antd Timeline mode="left";@aquarush/admin-ui 无 Timeline)——
  timeline: {
    listStyleType: 'none',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
  },
  timelineItem: {
    position: 'relative',
  },
  timelineHead: {
    position: 'absolute',
    top: '3px',
    left: '0px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    fontSize: '14px',
    // 动态:图标颜色由条目 color 决定,经 style={{}} 注入
  },
  timelineTail: {
    position: 'absolute',
    top: '19px',
    bottom: '0px',
    left: '7px',
    width: '2px',
    backgroundColor: tokens.border,
  },
  timelineContent: {
    paddingTop: '0px',
    paddingBottom: '12px',
    paddingLeft: '24px',
    paddingRight: '0px',
  },
});

// 操作类型到图标和颜色的映射
const OPERATION_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  ORDER_CREATED: {
    icon: <PlusOutlined />,
    color: 'blue',
    label: '订单创建',
  },
  ORDER_PAID: {
    icon: <DollarOutlined />,
    color: 'green',
    label: '支付成功',
  },
  ORDER_CANCELLED: {
    icon: <CloseCircleOutlined />,
    color: 'red',
    label: '订单取消',
  },
  ORDER_URGING: {
    icon: <ClockCircleOutlined />,
    color: 'orange',
    label: '用户催单',
  },
  DELIVERY_ASSIGNED: {
    icon: <UserOutlined />,
    color: 'cyan',
    label: '配送员分配',
  },
  DELIVERY_STARTED: {
    icon: <CarOutlined />,
    color: 'orange',
    label: '开始配送',
  },
  DELIVERY_COMPLETED: {
    icon: <CheckCircleOutlined />,
    color: 'green',
    label: '配送完成',
  },
  ORDER_CONFIRMED: {
    icon: <CheckCircleOutlined />,
    color: 'green',
    label: '用户确认收货(历史)',
  },
  AUTO_CONFIRMED: {
    icon: <CheckCircleOutlined />,
    color: 'green',
    label: '系统自动确认收货(历史)',
  },
  ORDER_COMPLETED: {
    icon: <CheckCircleOutlined />,
    color: 'green',
    label: '订单完成',
  },
  REFUND_INITIATED: {
    icon: <SafetyOutlined />,
    color: 'purple',
    label: '退款发起',
  },
  REFUND_COMPLETED: {
    icon: <SafetyOutlined />,
    color: 'purple',
    label: '退款完成',
  },
  DELIVERY_PHOTO_APPROVED: {
    icon: <CheckCircleOutlined />,
    color: 'green',
    label: '照片核验通过',
  },
  DELIVERY_PHOTO_REJECTED: {
    icon: <CloseCircleOutlined />,
    color: 'red',
    label: '照片核验驳回',
  },
  ADDRESS_UPDATED: {
    icon: <CarOutlined />,
    color: 'orange',
    label: '地址信息修正',
  },
  PAYMENT_TIMEOUT: {
    icon: <ClockCircleOutlined />,
    color: 'default',
    label: '支付超时',
  },
};

// 操作人类型标签
const OPERATOR_TYPE_TAG: Record<string, { text: string; color: string }> = {
  USER: { text: '用户', color: 'blue' },
  ADMIN: { text: '管理员', color: 'purple' },
  DELIVERY_WORKER: { text: '配送员', color: 'cyan' },
  SYSTEM: { text: '系统', color: 'default' },
};

export interface OrderOperation {
  // PrimaryId 序列化为字符串(后端 Long Snowflake)
  id: string;
  operationType: string;
  operatorId?: string;
  operatorType?: string;
  description?: string;
  extraData?: string;
  createdAt: string;
}

interface OrderOperationTimelineProps {
  operations: OrderOperation[];
}

const OrderOperationTimeline: React.FC<OrderOperationTimelineProps> = ({ operations }) => {
  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // 解析 extraData
  const parseExtraData = (extraData?: string) => {
    if (!extraData) return null;
    try {
      return JSON.parse(extraData);
    } catch {
      return null;
    }
  };

  // 按时间倒序排列
  const sortedOperations = [...operations].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );

  if (operations.length === 0) {
    return (
      <Card size="small" title="操作记录">
        <Text type="secondary">暂无操作记录</Text>
      </Card>
    );
  }

  return (
    <Card size="small" title="操作记录">
      <ol {...stylex.props(styles.timeline)}>
        {sortedOperations.map((operation) => {
          const config = OPERATION_CONFIG[operation.operationType] || {
            icon: <ClockCircleOutlined />,
            color: 'gray',
            label: operation.operationType,
          };
          const operatorTag = operation.operatorType
            ? OPERATOR_TYPE_TAG[operation.operatorType]
            : null;
          const extraDataObj = parseExtraData(operation.extraData);

          return (
            <li key={operation.id} {...stylex.props(styles.timelineItem)}>
              {/* 动态:图标颜色取决于条目 color */}
              <span
                {...stylex.props(styles.timelineHead)}
                style={{ color: TIMELINE_COLORS[config.color] ?? TIMELINE_COLORS.gray }}
              >
                {config.icon}
              </span>
              <div {...stylex.props(styles.timelineTail)} />
              <div {...stylex.props(styles.timelineContent)}>
                <div {...stylex.props(styles.item)}>
                  <div {...stylex.props(styles.itemHeader)}>
                    <Text strong>{config.label}</Text>
                    {operatorTag && (
                      <Tag color={operatorTag.color}>{operatorTag.text}</Tag>
                    )}
                  </div>
                  {operation.description && (
                    <Text type="secondary">{operation.description}</Text>
                  )}
                  {extraDataObj && (
                    <div {...stylex.props(styles.metaRow)}>
                      <JsonViewer value={extraDataObj} maxHeight={200} defaultExpandLevel={0} />
                    </div>
                  )}
                  <div {...stylex.props(styles.metaRow)}>
                    <Text type="secondary" {...stylex.props(styles.time)}>
                      {formatTime(operation.createdAt)}
                    </Text>
                  </div>
                </div>
              </div>
            </li>
          );
        })}
      </ol>
    </Card>
  );
};

export default OrderOperationTimeline;
