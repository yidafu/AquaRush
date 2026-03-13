import React from 'react';
import { Timeline, Card, Tag, Typography } from 'antd';
import {
  PlusOutlined,
  DollarOutlined,
  CloseCircleOutlined,
  UserOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SafetyOutlined,
} from '@ant-design/icons';

const { Text } = Typography;

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
  id: number;
  operationType: string;
  operatorId?: number;
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
      <Timeline
        mode="left"
        items={sortedOperations.map((operation) => {
          const config = OPERATION_CONFIG[operation.operationType] || {
            icon: <ClockCircleOutlined />,
            color: 'gray',
            label: operation.operationType,
          };
          const operatorTag = operation.operatorType
            ? OPERATOR_TYPE_TAG[operation.operatorType]
            : null;
          const extraDataObj = parseExtraData(operation.extraData);

          return {
            color: config.color,
            dot: config.icon,
            children: (
              <div style={{ paddingBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <Text strong>{config.label}</Text>
                  {operatorTag && (
                    <Tag color={operatorTag.color}>{operatorTag.text}</Tag>
                  )}
                </div>
                {operation.description && (
                  <Text type="secondary">{operation.description}</Text>
                )}
                {extraDataObj && (
                  <div style={{ marginTop: '4px' }}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      {JSON.stringify(extraDataObj)}
                    </Text>
                  </div>
                )}
                <div style={{ marginTop: '4px' }}>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    {formatTime(operation.createdAt)}
                  </Text>
                </div>
              </div>
            ),
          };
        })}
      />
    </Card>
  );
};

export default OrderOperationTimeline;
