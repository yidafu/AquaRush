import React, { useEffect } from 'react';
import { Card, Form, InputNumber, Button, Modal, Col, Row, Space } from '@aquarush/admin-ui';
import { centsToYuan, yuanToCents } from '@aquarush/common';
import { useDeliveryFeeRule, useSaveDeliveryFeeRule } from '../../../services/ops-graphql';
import * as stylex from '@stylexjs/stylex';
import RuleSummary from './components/RuleSummary';
import RuleFormSections from './components/RuleFormSections';
import type {
  DistanceTierFormValue,
  DistanceTierVo,
  FeeTierFormValue,
  FeeTierVo,
} from './types';

const styles = stylex.create({
  form: {
    maxWidth: '720px',
  },
  fullWidth: {
    width: '100%',
  },
});

// 配送费规则配置页(F-OPS-005 / R-MON-02 2026-10 分档修订 + R-OPS-03/G96 起送门槛与距离阶梯):
// 免费楼层内免运费;超过免费楼层按楼层所在档位加收(升序、区间不重叠、末档不封顶);
// 起送门槛(0=不启用)低于即拦下单;距离阶梯按门店与收货地址直线距离分档加收,与楼层分档叠加计算。
// 默认规则:免费4楼 + 5-6楼+2元 / 7-9楼+5元 / 10楼以上+8元;配置变更即时生效,已生成订单不受影响(运费按下单时快照)
const DeliveryFeeRule: React.FC = () => {
  const [form] = Form.useForm();
  const [loadRule, { data, loading }] = useDeliveryFeeRule();
  const [saveRule, { loading: saving }] = useSaveDeliveryFeeRule();

  useEffect(() => {
    loadRule();
  }, [loadRule]);

  // 回显当前规则(未配置时后端返回默认规则)
  useEffect(() => {
    const rule = data?.deliveryFeeRule;
    if (!rule) return;
    form.setFieldsValue({
      freeFloorLimit: rule.freeFloorLimit,
      surchargeYuan: centsToYuan(rule.surchargeCents),
      tiers: (rule.tiers ?? []).map((t: FeeTierVo) => ({
        fromFloor: t.fromFloor,
        toFloor: t.toFloor ?? undefined,
        feeYuan: centsToYuan(Number(t.feeCents)),
      })),
      minimumYuan: centsToYuan(Number(rule.minimumOrderCents ?? 0)),
      minimumEnabled: Number(rule.minimumOrderCents ?? 0) > 0,
      storeLongitude: rule.storeLongitude ?? undefined,
      storeLatitude: rule.storeLatitude ?? undefined,
      distanceTiers: (rule.distanceTiers ?? []).map((t: DistanceTierVo) => ({
        upToKm: t.upToKm ?? undefined,
        feeYuan: centsToYuan(Number(t.feeCents)),
      })),
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    const tiers = (values.tiers as FeeTierFormValue[] | undefined) ?? [];
    if (tiers.length === 0) {
      Modal.warning({ title: '请至少配置一个楼层档位' });
      return;
    }
    const minimumEnabled = values.minimumEnabled as boolean;
    if (minimumEnabled && !(values.minimumYuan > 0)) {
      Modal.warning({ title: '启用起送门槛时金额须大于 0' });
      return;
    }
    const distanceTiers = ((values.distanceTiers as DistanceTierFormValue[] | undefined) ?? []).filter(
      (t) => t && (t.upToKm != null || t.feeYuan != null)
    );
    const hasStoreCoordinate = values.storeLongitude != null && values.storeLatitude != null;
    if (distanceTiers.length > 0 && !hasStoreCoordinate) {
      Modal.warning({ title: '配置距离阶梯前请先填写门店经纬度' });
      return;
    }
    if (distanceTiers.some((t) => t.upToKm != null && t.upToKm <= 0)) {
      Modal.warning({ title: '距离上限须大于 0' });
      return;
    }
    for (let i = 1; i < distanceTiers.length; i++) {
      const prev = distanceTiers[i - 1].upToKm;
      const curr = distanceTiers[i].upToKm;
      if (curr == null && i !== distanceTiers.length - 1) {
        Modal.warning({ title: '不封顶档位(距离上限留空)必须为最后一档' });
        return;
      }
      if (prev != null && curr != null && curr <= prev) {
        Modal.warning({ title: '距离档位须按距离上限升序且区间不重叠' });
        return;
      }
    }
    Modal.confirm({
      title: '确定保存新的配送费规则吗?',
      content: '修改将影响新订单,已生成订单不受影响',
      okText: '确定保存',
      cancelText: '取消',
      onOk: async () => {
        await saveRule({
          variables: {
            input: {
              freeFloorLimit: values.freeFloorLimit,
              surchargeCents: yuanToCents(values.surchargeYuan),
              tiers: tiers.map((t) => ({
                fromFloor: t.fromFloor,
                toFloor: t.toFloor ?? null,
                feeCents: yuanToCents(t.feeYuan),
              })),
              minimumOrderCents: minimumEnabled ? yuanToCents(values.minimumYuan ?? 0) : 0,
              distanceTiers: distanceTiers.map((t) => ({
                upToKm: t.upToKm ?? null,
                feeCents: yuanToCents(t.feeYuan),
              })),
              storeLongitude: hasStoreCoordinate ? values.storeLongitude : null,
              storeLatitude: hasStoreCoordinate ? values.storeLatitude : null,
            },
          },
        });
      },
    });
  };

  return (
    <Card title="运费规则" loading={loading}>
      <RuleSummary rule={data?.deliveryFeeRule} />
      <Form form={form} layout="vertical" {...stylex.props(styles.form)} preserve={false}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="freeFloorLimit"
              label="免费楼层"
              rules={[{ required: true, message: '请输入免费楼层' }]}
              extra="楼层 ≤ 该值免运费,如 4 表示 4 楼及以下免费"
            >
              <InputNumber min={0} max={100} precision={0} {...stylex.props(styles.fullWidth)} placeholder="如 4" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="surchargeYuan"
              label="兜底单档加价(元)"
              rules={[
                { required: true, message: '请输入兜底加价' },
                {
                  validator: (_, value: number | undefined) =>
                    value !== undefined && value >= 0 && value <= 999 ? Promise.resolve() : Promise.reject(new Error('运费金额无效')),
                },
              ]}
              extra="仅当档位为空时参与计价的兼容字段"
            >
              <InputNumber min={0} max={999} precision={2} {...stylex.props(styles.fullWidth)} placeholder="如 2" />
            </Form.Item>
          </Col>
        </Row>

        <RuleFormSections />

        <Form.Item>
          <Space>
            <Button type="primary" onClick={handleSave} loading={saving}>
              保存
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default DeliveryFeeRule;
