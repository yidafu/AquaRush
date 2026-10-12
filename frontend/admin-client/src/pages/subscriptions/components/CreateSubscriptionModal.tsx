import React, { useEffect, useRef, useState } from 'react';
import { Form, Select, Row, Col, Spin } from '@/ui';
import { useLazyQuery } from '@apollo/client';
import type { Dayjs } from 'dayjs';
import type { UserVo } from '@aquarush/common';
import FormModal from '@/components/FormModal';
import { GET_USERS_QUERY } from '../../../graphql/queries/user.graphql';
import { SUBSCRIPTION_ADDRESSES_QUERY } from '../../../graphql/queries/subscription.graphql';
import type { SubscriptionAddressesQueryResult, SubscriptionWorkerOption } from '../types';
import { SubscriptionItemsField, SubscriptionScheduleFields } from './SubscriptionFormSections';

export interface CreateSubscriptionFormValues {
  userId: string;
  addressId: string;
  workerId?: string;
  items: { productId: string; quantity: number }[];
  cycleType: string;
  weekdays?: number[];
  monthDay?: number;
  timeSlot: string;
  startDate: Dayjs;
  endDate?: Dayjs | null;
  maxPeriods?: number | null;
  remark?: string;
}

/** 表单校验通过后交给页面提交的 payload(日期已转 yyyy-MM-dd,按周期类型裁剪字段) */
export interface CreateSubscriptionPayload {
  userId: string;
  addressId: string;
  workerId?: string;
  items: { productId: string; quantity: number }[];
  cycleType: string;
  weekdays?: number[];
  monthDay?: number;
  timeSlot: string;
  startDate: string;
  endDate?: string;
  maxPeriods?: number;
  remark?: string;
}

export interface CreateSubscriptionModalProps {
  open: boolean;
  /** 送水工下拉(列表页单页查询带回) */
  workers: SubscriptionWorkerOption[];
  submitting?: boolean;
  /** 表单校验通过后提交(页面负责调 createSubscription 并刷新) */
  onSubmit: (payload: CreateSubscriptionPayload) => void;
  onCancel: () => void;
}

/**
 * 新建周期计划弹窗(F-ORD-026/G77 后台代客下单):
 * 选用户(关键字搜索)→ 按用户拉地址 → 商品明细动态行(1~20)→ 周期/时段/起止日期。
 * 用户搜索 300ms 防抖;地址簿随选中用户懒加载;商品下拉复用 ProductSelect。
 */
const CreateSubscriptionModal: React.FC<CreateSubscriptionModalProps> = ({
  open,
  workers,
  submitting = false,
  onSubmit,
  onCancel,
}) => {
  const [form] = Form.useForm<CreateSubscriptionFormValues>();
  const cycleType = Form.useWatch('cycleType', form) || 'WEEKLY';

  // 用户搜索(防抖 300ms;IssueCouponModal 同款 lazy query 模式)
  const [userOptions, setUserOptions] = useState<{ value: string; label: string }[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchUsers] = useLazyQuery<{ users: { list: UserVo[] } }>(GET_USERS_QUERY, {
    fetchPolicy: 'network-only',
    errorPolicy: 'all',
  });
  const searchTimerRef = useRef<number | undefined>(undefined);

  // 地址簿:随选中用户懒加载(独立于列表页单页查询,选中用户后才发起)
  const [loadAddresses, { data: addressesData, loading: addressesLoading }] =
    useLazyQuery<SubscriptionAddressesQueryResult>(SUBSCRIPTION_ADDRESSES_QUERY, {
      fetchPolicy: 'network-only',
      errorPolicy: 'all',
    });

  const userId = Form.useWatch('userId', form);

  useEffect(() => {
    if (!open) return;
    form.resetFields();
  }, [open, form]);

  // 关闭动画结束后清空用户搜索结果(事件回调中 setState,避免 effect 内同步 setState)
  const handleAfterOpenChange = (opened: boolean) => {
    if (!opened) setUserOptions([]);
  };

  useEffect(() => {
    if (open && userId) {
      loadAddresses({ variables: { userId } });
      form.setFieldValue('addressId', undefined);
    }
  }, [open, userId, loadAddresses, form]);

  useEffect(() => () => window.clearTimeout(searchTimerRef.current), []);

  const handleUserSearch = (keyword: string) => {
    window.clearTimeout(searchTimerRef.current);
    searchTimerRef.current = window.setTimeout(() => {
      setSearching(true);
      searchUsers({ variables: { input: { search: keyword || undefined, page: 0, size: 20 } } })
        .then((result) => {
          const list = result.data?.users?.list ?? [];
          setUserOptions(
            list.map((u) => ({
              value: String(u.id),
              label: `${u.nickname || '未设置昵称'}${u.phone ? `(${u.phone})` : ''}`,
            })),
          );
        })
        .finally(() => setSearching(false));
    }, 300);
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit({
      userId: values.userId,
      addressId: values.addressId,
      workerId: values.workerId || undefined,
      items: values.items.map((item) => ({ productId: item.productId, quantity: item.quantity })),
      cycleType: values.cycleType,
      // 字段按周期类型裁剪:WEEKLY/BIWEEKLY 只带 weekdays,MONTHLY 只带 monthDay
      weekdays: values.cycleType === 'MONTHLY' ? undefined : values.weekdays,
      monthDay: values.cycleType === 'MONTHLY' ? values.monthDay : undefined,
      timeSlot: values.timeSlot,
      startDate: values.startDate.format('YYYY-MM-DD'),
      endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : undefined,
      maxPeriods: values.maxPeriods ?? undefined,
      remark: values.remark?.trim() || undefined,
    });
  };

  const addressOptions = (addressesData?.userAddresses ?? []).map((addr) => ({
    value: String(addr.id),
    label: `${addr.receiverName} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detailAddress}`,
  }));

  return (
    <FormModal
      open={open}
      record={null}
      submitting={submitting}
      onCancel={onCancel}
      onOk={handleOk}
      noun="周期订购计划"
      okText="创建"
      width={720}
      modalProps={{ afterOpenChange: handleAfterOpenChange }}
    >
      <Form
        form={form}
        layout="vertical"
        preserve={false}
        initialValues={{ cycleType: 'WEEKLY', timeSlot: 'IMMEDIATE', items: [{ quantity: 1 }] }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="userId"
              label="用户"
              rules={[{ required: true, message: '请选择用户' }]}
            >
              <Select
                showSearch
                filterOption={false}
                onSearch={handleUserSearch}
                onFocus={() => handleUserSearch('')}
                options={userOptions}
                loading={searching}
                placeholder="输入昵称/手机号搜索用户"
                notFoundContent={searching ? <Spin size="small" /> : '输入关键字搜索'}
                allowClear
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="addressId"
              label="收货地址"
              rules={[{ required: true, message: '请选择收货地址' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                options={addressOptions}
                loading={addressesLoading}
                placeholder={userId ? '选择收货地址' : '请先选择用户'}
                notFoundContent={addressesLoading ? <Spin size="small" /> : '该用户暂无收货地址'}
                disabled={!userId}
                allowClear
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item name="workerId" label="绑定送水工(可选)" tooltip="设置后到期定向提醒送水工;不设置则广播提醒">
          <Select
            showSearch
            optionFilterProp="label"
            placeholder="不设置则到期广播提醒"
            allowClear
            options={workers.map((worker) => ({
              value: worker.id,
              label: `${worker.name}(${worker.phone})`,
            }))}
          />
        </Form.Item>

        <SubscriptionItemsField />

        <SubscriptionScheduleFields cycleType={cycleType} />
      </Form>
    </FormModal>
  );
};

export default CreateSubscriptionModal;
