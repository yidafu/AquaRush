import React from 'react';
import { Avatar } from '@/ui';
import { UserOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { avatarFromName } from '@aquarush/common/utils';
import { SingleImageUpload } from '../ImageUpload';

// 局部色值 rgba(0,0,0,0.45) 为 antd 次级文字色,未入共享 token
const styles = stylex.create({
  hint: {
    marginTop: '8px',
    fontSize: '12px',
    color: 'rgba(0, 0, 0, 0.45)',
  },
  avatar: {
    marginLeft: '8px',
    verticalAlign: 'middle',
  },
});

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
      <div {...stylex.props(styles.hint)}>
        未上传头像时根据名称自动生成：
        <Avatar
          size={previewSize}
          shape="circle"
          src={avatarFromName(name || fallbackName)}
          icon={<UserOutlined />}
          {...stylex.props(styles.avatar)}
        />
      </div>
    )}
  </div>
);

export default AvatarUpload;
