import React, { useState } from 'react';
import { Upload, Button, Space, Image, message } from 'antd';
import { PlusOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd/es/upload';

interface ImageUploadProps {
  value?: string | string[];
  onChange?: (value: string | string[]) => void;
  multiple?: boolean;
  maxCount?: number;
}

export const ImageUpload: React.FC<ImageUploadProps> = ({
  value,
  onChange,
  multiple = false,
  maxCount = multiple ? 10 : 1,
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
      } as any);
    }
  };

  const handleRemove = (file: UploadFile) => {
    if (file.url) {
      const urls = multiple ? (value as string[] || []) : [value].filter(Boolean) as string[];
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
      const formData = new FormData();
      formData.append('file', file);
      formData.append('fileType', 'IMAGE');
      formData.append('isPublic', 'true');

      const response = await fetch('/api/v1/storage/files', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `上传失败: ${response.statusText}`);
      }

      const result = await response.json();

      if (!result.success) {
        throw new Error(result.message || '上传失败');
      }

      // 使用后端返回的fileUrl
      const fileUrl = result.data.fileUrl;

      setTimeout(() => {
        onSuccess?.({ url: fileUrl });
        message.success(`${file.name} 上传成功`);
      }, 500);
    } catch (error) {
      console.error('Upload error:', error);
      onError?.(error);
      message.error(`${file.name} 上传失败: ${error instanceof Error ? error.message : '未知错误'}`);
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
            onClick={() => Image.preview({ src: value as string } as any)}
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
