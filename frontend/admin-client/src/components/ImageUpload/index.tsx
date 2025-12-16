import React, { useState } from 'react';
import { Upload, Button, Space, Image, message } from 'antd';
import { PlusOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { RcFile, UploadFile, UploadProps } from 'antd/es/upload';

interface ImageUploadProps {
  value?: string | string[];
  onChange?: (value: string | string[]) => void;
  multiple?: boolean;
  maxCount?: number;
  title?: string;
}

export const ImageUpload: React.FC<ImageUploadProps> = ({
  value,
  onChange,
  multiple = false,
  maxCount = multiple ? 10 : 1,
  title = '图片上传'
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>(() => {
    if (!value) return [];

    if (multiple && Array.isArray(value)) {
      return value.map((url, index) => ({
        uid: `-${index}`,
        name: `image-${index}`,
        status: 'done' as const,
        url,
      }));
    } else if (!multiple && typeof value === 'string') {
      return [{
        uid: '-1',
        name: 'image',
        status: 'done' as const,
        url: value,
      }];
    }
    return [];
  });

  const handleChange: UploadProps['onChange'] = ({ fileList: newFileList }) => {
    // 只保留最新的文件，避免临时文件堆积
    const validFiles = newFileList.filter(file =>
      file.status === 'done' || file.status === 'uploading'
    );
    setFileList(validFiles);

    // 获取所有已完成的图片URL
    const completedUrls = validFiles
      .filter(file => file.status === 'done' && file.url)
      .map(file => file.url!);

    if (multiple) {
      onChange?.(completedUrls);
    } else {
      onChange?.(completedUrls[0] || '');
    }
  };

  const handlePreview = async (file: UploadFile) => {
    if (file.url) {
      // 使用 Ant Design 的 Image 预览功能
      Image.preview({
        src: file.url,
        width: 800,
      });
    }
  };

  const handleRemove = (file: UploadFile) => {
    if (file.url) {
      const urls = multiple ? (value as string[] || []) : [value].filter(Boolean);
      const newUrls = urls.filter(url => url !== file.url);

      if (multiple) {
        onChange?.(newUrls);
      } else {
        onChange?.('');
      }
    }
    return true;
  };

  const customRequest = async ({ file, onSuccess, onError }: any) => {
    try {
      // 这里应该实现实际的文件上传逻辑
      // 目前作为示例，我们创建一个临时URL
      const formData = new FormData();
      formData.append('file', file);

      // 模拟上传过程
      const mockUrl = `https://via.placeholder.com/400x400?text=${encodeURIComponent(file.name)}`;

      // 实际项目中应该调用真实的上传API
      // const response = await fetch('/api/upload', {
      //   method: 'POST',
      //   body: formData
      // });
      // const result = await response.json();
      // const url = result.url;

      setTimeout(() => {
        onSuccess?.({ url: mockUrl });
        message.success(`${file.name} 上传成功`);
      }, 1000);
    } catch (error) {
      onError?.(error);
      message.error(`${file.name} 上传失败`);
    }
  };

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传图片</div>
    </div>
  );

  return (
    <div className="image-upload">
      <Upload
        listType="picture-card"
        fileList={fileList}
        onPreview={handlePreview}
        onChange={handleChange}
        onRemove={handleRemove}
        customRequest={customRequest}
        multiple={multiple}
        maxCount={maxCount}
        accept="image/*"
        beforeUpload={(file) => {
          // 检查文件大小（限制为5MB）
          const isLt5M = file.size / 1024 / 1024 < 5;
          if (!isLt5M) {
            message.error('图片大小不能超过5MB!');
            return false;
          }

          // 检查文件类型
          const isImage = file.type.startsWith('image/');
          if (!isImage) {
            message.error('只能上传图片文件!');
            return false;
          }

          return true;
        }}
      >
        {fileList.length >= maxCount ? null : uploadButton}
      </Upload>

      {!multiple && value && (
        <Space style={{ marginTop: 8 }}>
          <Button
            icon={<EyeOutlined />}
            size="small"
            onClick={() => Image.preview({ src: value as string })}
          >
            预览
          </Button>
          <Button
            icon={<DeleteOutlined />}
            size="small"
            onClick={() => onChange?.('')}
          >
            删除
          </Button>
        </Space>
      )}
    </div>
  );
};

// 单图上传组件
export const SingleImageUpload: React.FC<Omit<ImageUploadProps, 'multiple'>> = (props) => (
  <ImageUpload {...props} multiple={false} maxCount={1} />
);

// 多图上传组件
export const MultiImageUpload: React.FC<Omit<ImageUploadProps, 'multiple'>> = (props) => (
  <ImageUpload {...props} multiple={true} maxCount={props.maxCount || 10} />
);