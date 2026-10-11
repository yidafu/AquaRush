import React, { useEffect } from 'react';
import { Card, Form, Input, Button, TimePicker, Alert, Space } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { SingleImageUpload } from '../../../components/ImageUpload';
import { useStoreProfile, useSaveStoreProfile } from '../../../services/ops-graphql';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  alert: {
    marginBottom: '24px',
  },
  form: {
    maxWidth: '560px',
  },
  fullWidth: {
    width: '100%',
  },
});

// 店铺信息配置页(F-OPS-004):单行配置表单,回显当前配置,保存后用户端关于页/反馈页动态读取
const StoreProfile: React.FC = () => {
  const [form] = Form.useForm();
  const [loadProfile, { data, loading }] = useStoreProfile();
  const [saveProfile, { loading: saving }] = useSaveStoreProfile();

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  // 回显当前配置(未配置时表单留空,保存即创建)
  useEffect(() => {
    const profile = data?.storeProfile;
    if (!profile) return;
    form.setFieldsValue({
      shopName: profile.shopName,
      logoUrl: profile.logoUrl || undefined,
      serviceHotline: profile.serviceHotline,
      businessHours:
        profile.businessHoursStart && profile.businessHoursEnd
          ? [dayjs(profile.businessHoursStart, 'HH:mm'), dayjs(profile.businessHoursEnd, 'HH:mm')]
          : undefined,
      shopIntro: profile.shopIntro || undefined,
      aboutUs: profile.aboutUs || undefined,
    });
  }, [data, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    const [start, end]: (Dayjs | undefined)[] = values.businessHours ?? [undefined, undefined];
    await saveProfile({
      variables: {
        input: {
          shopName: values.shopName,
          logoUrl: values.logoUrl || null,
          serviceHotline: values.serviceHotline,
          businessHoursStart: start ? start.format('HH:mm') : null,
          businessHoursEnd: end ? end.format('HH:mm') : null,
          shopIntro: values.shopIntro || null,
          aboutUs: values.aboutUs || null,
        },
      },
    });
  };

  return (
    <Card title="店铺信息" loading={loading}>
      <Alert
        type="info"
        showIcon
        {...stylex.props(styles.alert)}
        message="保存后立即生效:用户端关于页/意见反馈页将动态读取以下信息;未配置时用户端按默认值展示。每次修改留审计日志。"
      />
      <Form form={form} layout="vertical" {...stylex.props(styles.form)} preserve={false}>
        <Form.Item
          name="shopName"
          label="店铺名称"
          rules={[
            { required: true, message: '请输入店铺名称' },
            { max: 20, message: '店铺名称不能超过20个字' },
          ]}
        >
          <Input placeholder="请输入店铺名称" maxLength={20} showCount />
        </Form.Item>
        <Form.Item name="logoUrl" label="LOGO" extra="支持 jpg/png/webp,≤5MB">
          <SingleImageUpload />
        </Form.Item>
        <Form.Item
          name="serviceHotline"
          label="客服电话"
          rules={[
            { required: true, message: '请输入客服电话' },
            {
              pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$|^400-?\d{3}-?\d{4}$/,
              message: '客服电话格式不正确',
            },
          ]}
        >
          <Input placeholder="如 400-800-8888" />
        </Form.Item>
        <Form.Item name="businessHours" label="营业时间">
          <TimePicker.RangePicker format="HH:mm" {...stylex.props(styles.fullWidth)} />
        </Form.Item>
        <Form.Item
          name="shopIntro"
          label="店铺简介"
          rules={[{ max: 100, message: '店铺简介不能超过100个字' }]}
        >
          <Input placeholder="请输入店铺简介" maxLength={100} showCount />
        </Form.Item>
        <Form.Item
          name="aboutUs"
          label="关于我们"
          rules={[{ max: 2000, message: '关于我们内容不能超过2000个字' }]}
        >
          <Input.TextArea placeholder="请输入关于我们文案" maxLength={2000} showCount rows={6} />
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

export default StoreProfile;
