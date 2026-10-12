import React from 'react';
import {
  Button,
  Col,
  Form,
  InputNumber,
  Row,
  Switch,
  Typography,
} from '@aquarush/admin-ui';
import { PlusOutlined, MinusCircleOutlined } from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

const styles = stylex.create({
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
  // —— G96 起送门槛与距离阶梯区块(master 合并带入的静态内联迁入)——
  sectionTitle: {
    marginTop: '24px',
  },
  sectionRow: {
    marginTop: '16px',
  },
  hintBlock: {
    fontSize: '12px',
    display: 'block',
    marginTop: '4px',
  },
});

/**
 * 楼层档位(R-MON-02)+ 起送门槛与距离阶梯(R-OPS-03/G96)表单区块,
 * 由 index.tsx 拆分;需渲染在 <Form> 内(依赖 Form/List 上下文)。
 */
const RuleFormSections: React.FC = () => (
  <>
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

    <Typography.Paragraph strong {...stylex.props(styles.sectionTitle)}>
      起送门槛与距离阶梯运费(R-OPS-03)
    </Typography.Paragraph>
    <Row gutter={16} align="bottom">
      <Col span={8}>
        <Form.Item
          name="minimumEnabled"
          label="启用起送门槛"
          valuePropName="checked"
          extra="关闭时门槛为 0(不启用)"
          {...stylex.props(styles.noMarginBottom)}
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
              {...stylex.props(styles.noMarginBottom)}
            >
              <InputNumber
                min={0}
                max={9999}
                precision={2}
                {...stylex.props(styles.fullWidth)}
                placeholder="如 30.00"
                disabled={!getFieldValue('minimumEnabled')}
              />
            </Form.Item>
          )}
        </Form.Item>
      </Col>
      <Col span={6}>
        <Typography.Text type="secondary" {...stylex.props(styles.hint)}>
          订单商品金额低于门槛时服务端拦截下单并提示差额(自提单豁免)
        </Typography.Text>
      </Col>
    </Row>

    <Row gutter={16} {...stylex.props(styles.sectionRow)}>
      <Col span={12}>
        <Form.Item
          name="storeLongitude"
          label="门店经度(WGS84)"
          extra="距离阶梯计费基准;未配置则距离档不生效"
        >
          <InputNumber {...stylex.props(styles.fullWidth)} placeholder="如 113.930000" />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item name="storeLatitude" label="门店纬度(WGS84)">
          <InputNumber {...stylex.props(styles.fullWidth)} placeholder="如 22.530000" />
        </Form.Item>
      </Col>
    </Row>

    <Form.List name="distanceTiers">
      {(fields, { add, remove }, { errors }) => (
        <>
          {fields.map((field) => (
            <Row key={field.key} gutter={8} align="middle" {...stylex.props(styles.tierRow)}>
              <Col span={8}>
                <Form.Item
                  name={[field.name, 'upToKm']}
                  {...stylex.props(styles.noMarginBottom)}
                >
                  <InputNumber min={0.1} precision={1} placeholder="留空=不封顶(仅末档)" {...stylex.props(styles.fullWidth)} addonAfter="km" />
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
              <Col span={8} {...stylex.props(styles.rightAlign)}>
                <MinusCircleOutlined onClick={() => remove(field.name)} {...stylex.props(styles.removeIcon)} />
              </Col>
            </Row>
          ))}
          <Form.Item>
            <Button type="dashed" onClick={() => add({ upToKm: undefined, feeYuan: 0 })} block icon={<PlusOutlined />}>
              添加距离档位
            </Button>
            <Form.ErrorList errors={errors} />
            <Typography.Text type="secondary" {...stylex.props(styles.hintBlock)}>
              示例:≤3km +0 元 / ≤5km +3 元 / 5km 以上(上限留空)+6 元;按距离升序、区间不重叠,与楼层分档叠加计算,空表=不启用
            </Typography.Text>
          </Form.Item>
        </>
      )}
    </Form.List>
  </>
);

export default RuleFormSections;
