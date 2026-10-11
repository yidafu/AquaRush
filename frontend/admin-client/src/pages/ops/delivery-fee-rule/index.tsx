import React, { useEffect } from 'react';
import { Card, Form, InputNumber, Button, Modal, Alert, Space, Descriptions, Row, Col, Typography, Switch } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { formatDateTime, centsToYuan, yuanToCents } from '@aquarush/common';
import { useDeliveryFeeRule, useSaveDeliveryFeeRule } from '../../../services/ops-graphql';
import { COLOR_ERROR } from '../../../config/theme';

interface FeeTierFormValue {
  fromFloor: number;
  toFloor?: number | null;
  feeYuan: number;
}

interface FeeTierVo {
  fromFloor: number;
  toFloor?: number | null;
  feeCents: string | number;
}

interface DistanceTierFormValue {
  upToKm?: number | null;
  feeYuan: number;
}

interface DistanceTierVo {
  upToKm?: number | null;
  feeCents: string | number;
}

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

  const formatTier = (t: FeeTierVo): string => {
    const fee = centsToYuan(Number(t.feeCents)).toFixed(2);
    return t.toFloor == null ? `${t.fromFloor} 楼及以上 +${fee} 元` : `${t.fromFloor}-${t.toFloor} 楼 +${fee} 元`;
  };

  const formatDistanceTier = (t: DistanceTierVo): string => {
    const fee = centsToYuan(Number(t.feeCents)).toFixed(2);
    return t.upToKm == null ? `${fee} 元(不封顶)` : `≤${t.upToKm} km +${fee} 元`;
  };

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

  const rule = data?.deliveryFeeRule;
  const isDefault = !rule?.updatedAt;

  return (
    <Card title="运费规则" loading={loading}>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
        message="配送费按收货楼层分档计费,并可叠加距离阶梯加价(R-OPS-03):免费楼层及以下免运费,超过免费楼层按所在档位加收;距离档按门店与收货地址直线距离分档;起送门槛(0=不启用)低于即拦下单。配置变更即时生效,已生成订单不受影响;每次修改留审计日志。"
      />
      {isDefault && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 24 }}
          message="当前为默认规则(未配置):免费4楼,5-6楼+2元 / 7-9楼+5元 / 10楼及以上+8元"
        />
      )}
      {rule?.updatedAt && (
        <Descriptions size="small" style={{ marginBottom: 24 }} column={2}>
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
      <Form form={form} layout="vertical" style={{ maxWidth: 720 }} preserve={false}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="freeFloorLimit"
              label="免费楼层"
              rules={[{ required: true, message: '请输入免费楼层' }]}
              extra="楼层 ≤ 该值免运费,如 4 表示 4 楼及以下免费"
            >
              <InputNumber min={0} max={100} precision={0} style={{ width: '100%' }} placeholder="如 4" />
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
              <InputNumber min={0} max={999} precision={2} style={{ width: '100%' }} placeholder="如 2" />
            </Form.Item>
          </Col>
        </Row>

        <Typography.Paragraph strong>楼层档位(R-MON-02)</Typography.Paragraph>
        <Form.List
          name="tiers"
          rules={[
            {
              validator: async (_, value) => {
                if (!value || value.length === 0) {
                  return Promise.reject(new Error('请至少配置一个档位'));
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          {(fields, { add, remove }, { errors }) => (
            <>
              {fields.map((field, index) => (
                <Row key={field.key} gutter={8} align="middle" style={{ marginBottom: 8 }}>
                  <Col span={6}>
                    <Form.Item
                      name={[field.name, 'fromFloor']}
                      rules={[{ required: true, message: '起始楼层' }]}
                      style={{ marginBottom: 0 }}
                    >
                      <InputNumber min={1} precision={0} placeholder="起始楼层" style={{ width: '100%' }} addonAfter="楼" />
                    </Form.Item>
                  </Col>
                  <Col span={6}>
                    <Form.Item name={[field.name, 'toFloor']} style={{ marginBottom: 0 }}>
                      <InputNumber min={1} precision={0} placeholder="留空=不封顶" style={{ width: '100%' }} addonAfter="楼" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item
                      name={[field.name, 'feeYuan']}
                      rules={[{ required: true, message: '档位加价' }]}
                      style={{ marginBottom: 0 }}
                    >
                      <InputNumber min={0} max={999} precision={2} placeholder="加价金额" style={{ width: '100%' }} addonAfter="元" />
                    </Form.Item>
                  </Col>
                  <Col span={4} style={{ textAlign: 'right' }}>
                    {fields.length > 1 ? (
                      <MinusCircleOutlined onClick={() => remove(field.name)} style={{ color: COLOR_ERROR }} />
                    ) : null}
                  </Col>
                  {index === fields.length - 1 ? (
                    <Col span={24}>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        末档结束楼层必须留空(不封顶);起始楼层须严格升序且大于免费楼层,区间不可重叠
                      </Typography.Text>
                    </Col>
                  ) : null}
                </Row>
              ))}
              <Form.Item>
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加档位
                </Button>
                <Form.ErrorList errors={errors} />
              </Form.Item>
            </>
          )}
        </Form.List>

        <Typography.Paragraph strong style={{ marginTop: 24 }}>
          起送门槛与距离阶梯运费(R-OPS-03)
        </Typography.Paragraph>
        <Row gutter={16} align="bottom">
          <Col span={8}>
            <Form.Item
              name="minimumEnabled"
              label="启用起送门槛"
              valuePropName="checked"
              extra="关闭时门槛为 0(不启用)"
              style={{ marginBottom: 0 }}
            >
              <Switch />
            </Form.Item>
          </Col>
          <Col span={10}>
            <Form.Item noStyle shouldUpdate={(prev, curr) => prev.minimumEnabled !== curr.minimumEnabled}>
              {({ getFieldValue }) => (
                <Form.Item
                  name="minimumYuan"
                  label="起送门槛(元)"
                  rules={[
                    {
                      validator: (_, value: number | undefined) => {
                        if (!getFieldValue('minimumEnabled')) return Promise.resolve();
                        if (value !== undefined && value > 0 && value <= 9999) return Promise.resolve();
                        return Promise.reject(new Error('启用时门槛金额须 0-9999 元'));
                      },
                    },
                  ]}
                  style={{ marginBottom: 0 }}
                >
                  <InputNumber
                    min={0}
                    max={9999}
                    precision={2}
                    style={{ width: '100%' }}
                    placeholder="如 30.00"
                    disabled={!getFieldValue('minimumEnabled')}
                  />
                </Form.Item>
              )}
            </Form.Item>
          </Col>
          <Col span={6}>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              订单商品金额低于门槛时服务端拦截下单并提示差额(自提单豁免)
            </Typography.Text>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col span={12}>
            <Form.Item
              name="storeLongitude"
              label="门店经度(WGS84)"
              extra="距离阶梯计费基准;未配置则距离档不生效"
            >
              <InputNumber style={{ width: '100%' }} placeholder="如 113.930000" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="storeLatitude" label="门店纬度(WGS84)">
              <InputNumber style={{ width: '100%' }} placeholder="如 22.530000" />
            </Form.Item>
          </Col>
        </Row>

        <Form.List name="distanceTiers">
          {(fields, { add, remove }, { errors }) => (
            <>
              {fields.map((field) => (
                <Row key={field.key} gutter={8} align="middle" style={{ marginBottom: 8 }}>
                  <Col span={8}>
                    <Form.Item
                      name={[field.name, 'upToKm']}
                      style={{ marginBottom: 0 }}
                    >
                      <InputNumber min={0.1} precision={1} placeholder="留空=不封顶(仅末档)" style={{ width: '100%' }} addonAfter="km" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item
                      name={[field.name, 'feeYuan']}
                      rules={[{ required: true, message: '档位加价' }]}
                      style={{ marginBottom: 0 }}
                    >
                      <InputNumber min={0} max={999} precision={2} placeholder="加价金额" style={{ width: '100%' }} addonAfter="元" />
                    </Form.Item>
                  </Col>
                  <Col span={8} style={{ textAlign: 'right' }}>
                    <MinusCircleOutlined onClick={() => remove(field.name)} style={{ color: COLOR_ERROR }} />
                  </Col>
                </Row>
              ))}
              <Form.Item>
                <Button type="dashed" onClick={() => add({ upToKm: undefined, feeYuan: 0 })} block icon={<PlusOutlined />}>
                  添加距离档位
                </Button>
                <Form.ErrorList errors={errors} />
                <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
                  示例:≤3km +0 元 / ≤5km +3 元 / 5km 以上(上限留空)+6 元;按距离升序、区间不重叠,与楼层分档叠加计算,空表=不启用
                </Typography.Text>
              </Form.Item>
            </>
          )}
        </Form.List>

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
