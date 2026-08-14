import React, { useEffect, useState } from 'react';
import { Modal, Form, Radio, Select, Alert, message } from 'antd';
import { useLazyQuery } from '@apollo/client';
import type { CouponTemplate, IssueTargetType } from '@aquarush/common';
import { GET_USERS_QUERY } from '../../../../graphql/queries/user.graphql';
import { useCustomerGroups, useIssueCoupons } from '../../../../services/marketing-graphql';

interface Props {
  open: boolean;
  couponTemplate: CouponTemplate | null;
  onCancel: () => void;
}

const TARGET_TYPE_OPTIONS = [
  { value: 'USER', label: '按用户发放' },
  { value: 'GROUP', label: '按客户分组发放' },
];

// 定向发放优惠券(F-MKT-003):按用户列表 / 客户分组发放,返回实际发放张数
const IssueCouponModal: React.FC<Props> = ({ open, couponTemplate, onCancel }) => {
  const [form] = Form.useForm();
  const [targetType, setTargetType] = useState<IssueTargetType>('USER');
  const [userOptions, setUserOptions] = useState<{ value: string; label: string }[]>([]);
  const [searchUsers, { loading: searching }] = useLazyQuery(GET_USERS_QUERY, {
    fetchPolicy: 'network-only',
    errorPolicy: 'all',
  });
  const [loadGroups, { data: groupsData }] = useCustomerGroups();
  const [issueCoupons, { loading: issuing }] = useIssueCoupons();

  useEffect(() => {
    if (!open) return;
    form.resetFields();
    setTargetType('USER');
    setUserOptions([]);
    loadGroups({ variables: { input: { page: 0, size: 200 } } });
  }, [open, form, loadGroups]);

  const handleUserSearch = (keyword: string) => {
    searchUsers({
      variables: { input: { search: keyword || undefined, page: 0, size: 50 } },
    }).then((result) => {
      const list = result.data?.users?.list ?? [];
      setUserOptions(
        list.map((u) => ({
          value: String(u.id),
          label: `${u.nickname || '未设置昵称'}${u.phone ? `(${u.phone})` : ''}`,
        })),
      );
    });
  };

  const handleOk = async () => {
    if (!couponTemplate) return;
    const values = await form.validateFields();
    const result = await issueCoupons({
      variables: {
        input: {
          couponTemplateId: couponTemplate.id,
          targetType: values.targetType,
          userIds: values.targetType === 'USER' ? (values.userIds ?? []) : null,
          customerGroupId: values.targetType === 'GROUP' ? (values.customerGroupId ?? null) : null,
        },
      },
    });
    const count = result.data?.issueCoupons ?? 0;
    message.success(`成功发放 ${count} 张优惠券`);
    onCancel();
  };

  const groups = groupsData?.customerGroups?.content ?? [];

  return (
    <Modal
      title={`定向发放 - ${couponTemplate?.name ?? ''}`}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={issuing}
      destroyOnClose
      width={520}
      okText="发放"
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="按用户发放时按用户列表逐个发放;按分组发放时对该分组当前全部成员发放。系统自动跳过已超限领数的用户,返回实际发放张数。"
      />
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="targetType"
          label="发放方式"
          initialValue="USER"
          rules={[{ required: true, message: '请选择发放方式' }]}
        >
          <Radio.Group options={TARGET_TYPE_OPTIONS} onChange={(e) => setTargetType(e.target.value)} />
        </Form.Item>
        {targetType === 'USER' ? (
          <Form.Item name="userIds" label="选择用户" rules={[{ required: true, message: '请选择用户' }]}>
            <Select
              mode="multiple"
              showSearch
              filterOption={false}
              onSearch={handleUserSearch}
              onFocus={() => handleUserSearch('')}
              options={userOptions}
              loading={searching}
              placeholder="输入昵称/手机号搜索用户"
              style={{ width: '100%' }}
              maxTagCount="responsive"
            />
          </Form.Item>
        ) : (
          <Form.Item name="customerGroupId" label="选择客户分组" rules={[{ required: true, message: '请选择客户分组' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              options={groups.map((g) => ({ value: String(g.id), label: `${g.name}(${g.memberCount}人)` }))}
              placeholder="选择客户分组"
            />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default IssueCouponModal;
