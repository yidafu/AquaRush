import React from 'react';
import { Modal, Form, Input, Checkbox, message } from 'antd';
import type { Address, AddressFormData } from './types';
import { RegionCascader } from '../../../components/RegionCascader';
import type { RegionCascaderValue } from '../../../components/RegionCascader';
import AddressMapPicker from '../../../components/AddressMapPicker';

interface AddressFormProps {
  visible: boolean;
  loading?: boolean;
  record?: Address | null;
  onCancel: () => void;
  onSubmit: (values: AddressFormData) => Promise<void>;
}

export const AddressForm: React.FC<AddressFormProps> = ({
  visible,
  loading = false,
  record,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<AddressFormData>();
  // Enable auto geocode when:
  // - Creating new address, OR
  // - Editing address without existing coordinates
  const [autoLocate, setAutoLocate] = React.useState(false);

  React.useEffect(() => {
    if (visible) {
      if (record) {
        form.setFieldsValue({
          receiverName: record.receiverName,
          phone: record.phone,
          province: record.province,
          provinceCode: record.provinceCode,
          city: record.city,
          cityCode: record.cityCode,
          district: record.district,
          districtCode: record.districtCode,
          detailAddress: record.detailAddress,
          longitude: record.longitude,
          latitude: record.latitude,
          isDefault: record.isDefault,
        });
        // Auto locate if no existing coordinates
        setAutoLocate(!record.longitude || !record.latitude);
      } else {
        form.resetFields();
        form.setFieldsValue({
          isDefault: false,
        });
        // Always auto locate for new addresses
        setAutoLocate(true);
      }
    }
  }, [visible, record, form]);

  // Handle region selection change
  const handleRegionChange = React.useCallback((value: RegionCascaderValue) => {
    form.setFieldsValue({
      province: value.province,
      provinceCode: value.provinceCode,
      city: value.city,
      cityCode: value.cityCode,
      district: value.district,
      districtCode: value.districtCode,
    });
  }, [form]);

  // Handle map location change
  const handleLocationChange = React.useCallback((location: {
    longitude: number;
    latitude: number;
    address?: string;
  }) => {
    form.setFieldsValue({
      longitude: location.longitude,
      latitude: location.latitude,
    });
  }, [form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
      form.resetFields();
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  // Get current form values for map component
  const formValues = form.getFieldsValue([
    'province',
    'city',
    'district',
    'detailAddress',
    'longitude',
    'latitude',
  ]);

  return (
    <Modal
      title={record ? '编辑地址' : '新增地址'}
      open={visible}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={loading}
      width={720}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          isDefault: false,
        }}
      >
        <Form.Item
          name="receiverName"
          label="收货人姓名"
          rules={[{ required: true, message: '请输入收货人姓名' }]}
        >
          <Input placeholder="请输入收货人姓名" />
        </Form.Item>

        <Form.Item
          name="phone"
          label="手机号"
          rules={[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
          ]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>

        <Form.Item
          name="region"
          label="省市区"
          rules={[{ required: true, message: '请选择省/市/区' }]}
          tooltip="选择省市区后，地图将自动定位"
        >
          <RegionCascader
            value={{
              province: formValues.province || '',
              city: formValues.city || '',
              district: formValues.district || '',
              provinceCode: formValues.provinceCode,
              cityCode: formValues.cityCode,
              districtCode: formValues.districtCode,
            }}
            onChange={handleRegionChange}
            placeholder="请选择省/市/区"
          />
        </Form.Item>

        <Form.Item
          name="detailAddress"
          label="详细地址"
          rules={[{ required: true, message: '请输入详细地址' }]}
        >
          <Input.TextArea
            placeholder="请输入详细地址，如街道、门牌号、小区、楼号等"
            rows={2}
          />
        </Form.Item>

        {/* Hidden fields to store region codes */}
        <Form.Item name="province" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="provinceCode" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="city" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="cityCode" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="district" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="districtCode" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          name="location"
          label="地图定位"
          tooltip="点击定位按钮根据地址自动获取坐标，也可手动输入经纬度"
        >
          <AddressMapPicker
            province={formValues.province}
            city={formValues.city}
            district={formValues.district}
            detailAddress={formValues.detailAddress}
            longitude={formValues.longitude}
            latitude={formValues.latitude}
            onChange={handleLocationChange}
            autoLocate={autoLocate}
          />
        </Form.Item>

        {/* Hidden fields to store coordinates */}
        <Form.Item name="longitude" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="latitude" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          name="isDefault"
          valuePropName="checked"
        >
          <Checkbox>设为默认地址</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  );
};
