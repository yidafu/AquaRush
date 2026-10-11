import React, { useEffect } from 'react';
import { Card, Form, InputNumber, Button, Modal, Alert, Space, Descriptions, Row, Col, Typography } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { formatDateTime, centsToYuan, yuanToCents } from '@aquarush/common';
import { useDeliveryFeeRule, useSaveDeliveryFeeRule } from '../../../services/ops-graphql';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

const styles = stylex.create({
  alert: {
    marginBottom: '24px',
  },
  desc: {
    marginBottom: '24px',
  },
  form: {
    maxWidth: '720px',
  },
  fullWidth: {
    width: '100%',
  },
  tierRow: {
    marginBottom: '8px',
  },
  noMarginBottom: {
    marginBottom: '0px',
  },
  rightAlign: {
    textAlign: 'right',
  },
  removeIcon: {
    color: tokens.error,
  },
  hint: {
    fontSize: '12px',
  },
});

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

// 配送费规则配置页(F-OPS-005 / R-MON-02 2026-10 分档修订):
// 免费楼层内免运费;超过免费楼层按楼层所在档位加收(升序、区间不重叠、末档不封顶)。
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
    });
  }, [data, form]);

  const formatTier = (t: FeeTierVo): string => {
    const fee = centsToYuan(Number(t.feeCents)).toFixed(2);
    return t.toFloor == null ? `${t.fromFloor} 楼及以上 +${fee} 元` : `${t.fromFloor}-${t.toFloor} 楼 +${fee} 元`;
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    const tiers = (values.tiers as FeeTierFormValue[] | undefined) ?? [];
    if (tiers.length === 0) {
      Modal.warning({ title: '请至少配置一个楼层档位' });
      return;
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
        {...stylex.props(styles.alert)}
        message="配送费按收货楼层分档计费:免费楼层及以下免运费,超过免费楼层按所在档位加收(档位升序、区间不重叠、末档不封顶)。配置变更即时生效,已生成订单不受影响;每次修改留审计日志。"
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
          <Descriptions.Item label="最近更新">
            {rule.updatedAt ? formatDateTime(rule.updatedAt) : '—'}
          </Descriptions.Item>
        </Descriptions>
      )}
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
                <Row key={field.key} gutter={8} align="middle" {...stylex.props(styles.tierRow)}>
                  <Col span={6}>
                    <Form.Item
                      name={[field.name, 'fromFloor']}
                      rules={[{ required: true, message: '起始楼层' }]}
                      {...stylex.props(styles.noMarginBottom)}
                    >
                      <InputNumber min={1} precision={0} placeholder="起始楼层" {...stylex.props(styles.fullWidth)} addonAfter="楼" />
                    </Form.Item>
                  </Col>
                  <Col span={6}>
                    <Form.Item name={[field.name, 'toFloor']} {...stylex.props(styles.noMarginBottom)}>
                      <InputNumber min={1} precision={0} placeholder="留空=不封顶" {...stylex.props(styles.fullWidth)} addonAfter="楼" />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item
                      name={[field.name, 'feeYuan']}
                      rules={[{ required: true, message: '档位加价' }]}
                      {...stylex.props(styles.noMarginBottom)}
                    >
                      <InputNumber min={0} max={999} precision={2} placeholder="加价金额" {...stylex.props(styles.fullWidth)} addonAfter="元" />
                    </Form.Item>
                  </Col>
                  <Col span={4} {...stylex.props(styles.rightAlign)}>
                    {fields.length > 1 ? (
                      <MinusCircleOutlined onClick={() => remove(field.name)} {...stylex.props(styles.removeIcon)} />
                    ) : null}
                  </Col>
                  {index === fields.length - 1 ? (
                    <Col span={24}>
                      <Typography.Text type="secondary" {...stylex.props(styles.hint)}>
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
