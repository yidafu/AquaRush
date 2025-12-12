import React from 'react';
import { Form, Input, Select, Switch, Modal, Upload } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { DeliveryWorkerFormData, FormModalProps } from './types';
import type { UploadFile, UploadProps } from 'antd/es/upload/interface';

const DeliveryWorkerForm: React.FC<FormModalProps<DeliveryWorkerFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm();
  const [fileList, setFileList] = React.useState<UploadFile[]>([]);

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        name: record.name,
        phone: record.phone,
        wechatOpenId: record.wechatOpenId,
        avatarUrl: record.avatarUrl,
        status: record.status,
        isAvailable: record.isAvailable,
      });

      if (record.avatarUrl) {
        setFileList([
          {
            uid: '-1',
            name: 'avatar.jpg',
            status: 'done',
            url: record.avatarUrl,
          },
        ]);
      }
    } else {
      form.resetFields();
      form.setFieldsValue({
        status: 'OFFLINE',
        isAvailable: true,
      });
      setFileList([]);
    }
  }, [record, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // Handle avatar URL from upload if present
      if (fileList.length > 0 && fileList[0].url) {
        values.avatarUrl = fileList[0].url;
      }

      await onSubmit(values);
      form.resetFields();
      setFileList([]);
    } catch (error) {
      console.error('Form validation failed:', error);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setFileList([]);
    onClose();
  };

  const uploadProps: UploadProps = {
    name: 'file',
    listType: 'picture-card',
    fileList,
    maxCount: 1,
    accept: 'image/*',
    beforeUpload: (file) => {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isJpgOrPng) {
        console.error('只能上传 JPG/PNG 格式的图片!');
        return false;
      }
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isLt2M) {
        console.error('图片大小不能超过 2MB!');
        return false;
      }
      return false; // Prevent auto upload
    },
    onChange: (info) => {
      setFileList(info.fileList.slice(-1));
    },
    onRemove: () => {
      setFileList([]);
    },
  };

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传头像</div>
    </div>
  );

  return (
    <Modal
      title={record ? '编辑送水员' : '新增送水员'}
      open={open}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={700}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          status: 'OFFLINE',
          isAvailable: true,
        }}
      >
        <Form.Item
          label="头像"
          name="avatarUrl"
        >
          <Upload {...uploadProps}>
            {fileList.length >= 1 ? null : uploadButton}
          </Upload>
        </Form.Item>

        <Form.Item
          label="姓名"
          name="name"
          rules={[
            { required: true, message: '请输入姓名' },
            { min: 2, max: 20, message: '姓名长度为2-20个字符' },
            { pattern: /^[\u4e00-\u9fa5a-zA-Z\s]+$/, message: '姓名只能包含中文、英文字母和空格' },
          ]}
        >
          <Input placeholder="请输入姓名" />
        </Form.Item>

        <Form.Item
          label="手机号"
          name="phone"
          rules={[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' },
          ]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>

        <Form.Item
          label="微信OpenID"
          name="wechatOpenId"
          rules={[
            { required: true, message: '请输入微信OpenID' },
            { min: 1, max: 100, message: '微信OpenID长度为1-100个字符' },
          ]}
        >
          <Input placeholder="请输入微信OpenID" />
        </Form.Item>

        <Form.Item
          label="状态"
          name="status"
          rules={[{ required: true, message: '请选择状态' }]}
        >
          <Select placeholder="请选择状态">
            <Select.Option value="ONLINE">在线</Select.Option>
            <Select.Option value="OFFLINE">离线</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="是否可接单"
          name="isAvailable"
          valuePropName="checked"
        >
          <Switch checkedChildren="可接单" unCheckedChildren="不可接单" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DeliveryWorkerForm;