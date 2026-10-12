import React from 'react';
import { Alert, Descriptions } from '@/ui';
import { formatDateTime, centsToYuan } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import type { DeliveryFeeRuleData, DistanceTierVo, FeeTierVo } from '../types';

const styles = stylex.create({
  alert: {
    marginBottom: '24px',
  },
  desc: {
    marginBottom: '24px',
  },
});

const formatTier = (t: FeeTierVo): string => {
  const fee = centsToYuan(Number(t.feeCents)).toFixed(2);
  return t.toFloor == null ? `${t.fromFloor} 楼及以上 +${fee} 元` : `${t.fromFloor}-${t.toFloor} 楼 +${fee} 元`;
};

const formatDistanceTier = (t: DistanceTierVo): string => {
  const fee = centsToYuan(Number(t.feeCents)).toFixed(2);
  return t.upToKm == null ? `${fee} 元(不封顶)` : `≤${t.upToKm} km +${fee} 元`;
};

export interface RuleSummaryProps {
  rule: DeliveryFeeRuleData | undefined;
}

/** 说明 Alert + 当前规则回显 Descriptions(由 index.tsx 拆分) */
const RuleSummary: React.FC<RuleSummaryProps> = ({ rule }) => {
  const isDefault = !rule?.updatedAt;
  return (
    <>
      <Alert
        type="info"
        showIcon
        {...stylex.props(styles.alert)}
        message="配送费按收货楼层分档计费,并可叠加距离阶梯加价(R-OPS-03):免费楼层及以下免运费,超过免费楼层按所在档位加收;距离档按门店与收货地址直线距离分档;起送门槛(0=不启用)低于即拦下单。配置变更即时生效,已生成订单不受影响;每次修改留审计日志。"
      />
      {isDefault && (
        <Alert
          type="warning"
          showIcon
          {...stylex.props(styles.alert)}
          message="当前为默认规则(未配置):免费4楼,5-6楼+2元 / 7-9楼+5元 / 10楼及以上+8元"
        />
      )}
      {rule?.updatedAt && (
        <Descriptions size="small" {...stylex.props(styles.desc)} column={2}>
          <Descriptions.Item label="当前免费楼层">{rule.freeFloorLimit} 楼</Descriptions.Item>
          <Descriptions.Item label="兜底单档加价">
            {(centsToYuan(rule.surchargeCents)).toFixed(2)} 元(仅档位为空时生效)
          </Descriptions.Item>
          <Descriptions.Item label="当前档位" span={2}>
            {(rule.tiers ?? []).length > 0
              ? (rule.tiers ?? []).map(formatTier).join('  |  ')
              : '未配置(计价回退兜底单档)'}
          </Descriptions.Item>
          <Descriptions.Item label="起送门槛">
            {Number(rule.minimumOrderCents ?? 0) > 0 ? `${centsToYuan(Number(rule.minimumOrderCents)).toFixed(2)} 元` : '未启用'}
          </Descriptions.Item>
          <Descriptions.Item label="距离阶梯" span={2}>
            {(rule.distanceTiers ?? []).length > 0
              ? (rule.distanceTiers ?? []).map(formatDistanceTier).join('  |  ')
              : '未启用'}
          </Descriptions.Item>
          <Descriptions.Item label="最近更新">
            {rule.updatedAt ? formatDateTime(rule.updatedAt) : '—'}
          </Descriptions.Item>
        </Descriptions>
      )}
    </>
  );
};

export default RuleSummary;
