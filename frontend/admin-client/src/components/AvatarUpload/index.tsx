import React from 'react';
import { Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { avatarFromName } from '@aquarush/common/utils';
import { SingleImageUpload } from '../ImageUpload';

interface AvatarUploadProps {
  value?: string;
  onChange?: (value?: string) => void;
  /** 用于生成默认头像的名称(用户名/昵称/姓名);未上传头像时按此生成 GitHub 风格 identicon 预览 */
  name?: string | null;
  /** name 为空时的兜底名称(如 '送水员' / '用户') */
  fallbackName?: string;
  /** 未上传时预览的默认头像尺寸(px) */
  previewSize?: number;
}

/**
 * 头像上传(公共组件):图片上传 + 未上传时根据名称自动生成默认头像的预览。
 * 三处表单(账号/用户/送水员)复用;可直接作为 Form.Item 的受控组件(value/onChange)。
 */
export const AvatarUpload: React.FC<AvatarUploadProps> = ({
  value,
  onChange,
  name,
  fallbackName = '用户',
  previewSize = 28,
}) => (
  <div>
    <SingleImageUpload
      value={value}
      onChange={(v) => onChange?.(typeof v === 'string' ? v : undefined)}
    />
    {!value && (
      <div style={{ marginTop: 8, fontSize: 12, color: 'rgba(0, 0, 0, 0.45)' }}>
        未上传头像时根据名称自动生成：
        <Avatar
          size={previewSize}
          shape="circle"
          src={avatarFromName(name || fallbackName)}
          icon={<UserOutlined />}
          style={{ marginLeft: 8, verticalAlign: 'middle' }}
        />
      </div>
    )}
  </div>
);

export default AvatarUpload;
