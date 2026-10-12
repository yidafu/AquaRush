import React from 'react';
import { Button, Space, Typography } from '@aquarush/admin-ui';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
} from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  title: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
  },
});

const { Title } = Typography;

export interface ProductFormHeaderProps {
  pageTitle: string;
  /** 详情模式(只读)才显示「编辑商品」 */
  isView: boolean;
  /** 新增/编辑模式显示「取消/保存」 */
  editable: boolean;
  submitting: boolean;
  onBack: () => void;
  onEdit: () => void;
  onCancel: () => void;
  onSave: () => void;
}

/** 商品表单页头部:返回 + 标题 + 操作按钮 */
export const ProductFormHeader: React.FC<ProductFormHeaderProps> = ({
  pageTitle,
  isView,
  editable,
  submitting,
  onBack,
  onEdit,
  onCancel,
  onSave,
}) => (
  <div {...stylex.props(styles.header)}>
    <Space>
      <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
        返回列表
      </Button>
      <Title level={2} {...stylex.props(styles.title)}>
        {pageTitle}
      </Title>
    </Space>
    <Space>
      {isView && (
        <Button
          type="primary"
          icon={<EditOutlined />}
          onClick={onEdit}
        >
          编辑商品
        </Button>
      )}
      {editable && (
        <>
          <Button
            icon={<CloseOutlined />}
            onClick={onCancel}
          >
            取消
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            loading={submitting}
            onClick={onSave}
          >
            保存
          </Button>
        </>
      )}
    </Space>
  </div>
);
