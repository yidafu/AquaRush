import { SystemSettingKeys } from '@aquarush/common';

/** 通知与订阅消息配置页(F-NTF-005)共享常量与类型 */

export interface TemplateScenario {
  key: string;
  label: string;
  description: string;
  idKey: string;
  enabledKey: string;
}

export const SCENARIOS: TemplateScenario[] = [
  {
    key: 'order_created',
    label: '下单通知',
    description: '用户下单成功后推送订单确认',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_CREATED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_CREATED_ENABLED,
  },
  {
    key: 'delivery_assigned',
    label: '配送员接单',
    description: '配送员接单后推送',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_ASSIGNED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_ASSIGNED_ENABLED,
  },
  {
    key: 'delivery_started',
    label: '开始配送',
    description: '开始配送后推送',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_STARTED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_STARTED_ENABLED,
  },
  {
    key: 'delivery_completed',
    label: '配送完成',
    description: '配送完成后推送送达确认',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ENABLED,
  },
  {
    key: 'order_urging',
    label: '催单',
    description: '配送超时用户催单提醒',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_URGING_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_ORDER_URGING_ENABLED,
  },
  {
    key: 'payment_result',
    label: '支付结果',
    description: '支付成功/失败结果通知',
    idKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ID,
    enabledKey: SystemSettingKeys.NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ENABLED,
  },
];

// 微信模板 ID:字母/数字/下划线/中划线,6-64 位(宽松校验,避免误拦)
export const TEMPLATE_ID_PATTERN = /^[A-Za-z0-9_-]{6,64}$/;

export interface DeliveryPushScenario {
  key: string;
  label: string;
  description: string;
  enabledKey: string;
}

// 送水 App 实时推送场景(F-NTF-003,PRD 13-notification.md):任务关键节点
export const DELIVERY_PUSH_SCENARIOS: DeliveryPushScenario[] = [
  {
    key: 'new_order',
    label: '新订单',
    description: '新订单产生(可抢单),任务列表即时置顶',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_NEW_ORDER_ENABLED,
  },
  {
    key: 'assigned',
    label: '被派单',
    description: '订单派给本人时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_ASSIGNED_ENABLED,
  },
  {
    key: 'cancelled',
    label: '订单取消',
    description: '已接订单被取消时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_CANCELLED_ENABLED,
  },
  {
    key: 'urging',
    label: '用户催单',
    description: '用户催单时推送',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_URGING_ENABLED,
  },
  {
    key: 'timeout',
    label: '配送超时',
    description: '配送超时未送达提醒',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_TIMEOUT_ENABLED,
  },
  {
    key: 'payment_timeout',
    label: '支付超时',
    description: '待支付订单超时未支付提醒',
    enabledKey: SystemSettingKeys.DELIVERY_PUSH_PAYMENT_TIMEOUT_ENABLED,
  },
];

export interface TemplateRow {
  scenario: TemplateScenario;
  templateId: string | null;
  enabled: boolean;
}
