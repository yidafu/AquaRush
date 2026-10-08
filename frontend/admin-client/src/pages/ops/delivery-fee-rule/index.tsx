import React, { useEffect } from 'react';
import { Card, Form, InputNumber, Button, Modal, Alert, Space, Descriptions } from 'antd';
import { formatDateTime, centsToYuan, yuanToCents } from '@aquarush/common';
import { useDeliveryFeeRule, useSaveDeliveryFeeRule } from '../../../services/ops-graphql';

// 配送费规则配置页(F-OPS-005):楼层口径 —— 免费楼层内免运费,超过免费楼层一次性加收固定金额
// 默认规则:免费4楼/加价2元;配置变更即时生效,已生成订单不受影响(运费按下单时快照)
const DeliveryFeeRule: React.FC = () => {
  const [form] = Form.useForm();
  const [loadRule, { data, loading }] = useDeliveryFeeRule();
  const [saveRule, { loading: saving }] = useSaveDeliveryFeeRule();

  useEffect(() => {
    loadRule();
  }, [loadRule]);

  // 回显当前规则(未配置时后端返回默认规则 免费4楼/加价2元)
  useEffect(() => {
    const rule = data?.deliveryFeeRule;
    if (!rule) return;
    form.setFieldsValue({
      freeFloorLimit: rule.freeFloorLimit,
      surchargeYuan: centsToYuan(rule.surchargeCents),
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
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
        message="配送费由收货楼层决定:免费楼层及以下免运费,超过免费楼层的订单一次性加收固定金额(不随层数累加)。配置变更即时生效,已生成订单不受影响;每次修改留审计日志。"
      />
      {isDefault && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 24 }}
          message="当前为默认规则(未配置):免费4楼,超出加价2元"
        />
      )}
      {rule?.updatedAt && (
        <Descriptions size="small" style={{ marginBottom: 24 }} column={2}>
          <Descriptions.Item label="当前免费楼层">{rule.freeFloorLimit} 楼</Descriptions.Item>
          <Descriptions.Item label="当前超出加价">{(centsToYuan(rule.surchargeCents)).toFixed(2)} 元</Descriptions.Item>
          <Descriptions.Item label="最近更新">
            {rule.updatedAt ? formatDateTime(rule.updatedAt) : '—'}
          </Descriptions.Item>
        </Descriptions>
      )}
      <Form form={form} layout="vertical" style={{ maxWidth: 560 }} preserve={false}>
        <Form.Item
          name="freeFloorLimit"
          label="免费楼层"
          rules={[{ required: true, message: '请输入免费楼层' }]}
          extra="楼层 ≤ 该值免运费,如 4 表示 4 楼及以下免费"
        >
          <InputNumber min={0} max={100} precision={0} style={{ width: '100%' }} placeholder="如 4" />
        </Form.Item>
        <Form.Item
          name="surchargeYuan"
          label="超出加价(元)"
          rules={[
            { required: true, message: '请输入超出加价' },
            {
              validator: (_, value: number | undefined) =>
                value !== undefined && value >= 0 && value <= 999 ? Promise.resolve() : Promise.reject(new Error('运费金额无效')),
            },
          ]}
          extra="超过免费楼层的订单一次性加收,0-999 元,≤2 位小数"
        >
          <InputNumber min={0} max={999} precision={2} style={{ width: '100%' }} placeholder="如 2" />
        </Form.Item>
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
