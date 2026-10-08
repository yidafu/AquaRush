import React from 'react';
import { Card, Button, Space } from 'antd';
import { ClearOutlined } from '@ant-design/icons';

export interface FilterCardProps {
  /** 重置按钮点击(通常接 useDebouncedFilters.reset) */
  onReset: () => void;
  /** 右上角自定义操作(重置按钮左侧,如导出/批量导入) */
  extra?: React.ReactNode;
  style?: React.CSSProperties;
  children: React.ReactNode;
}

/**
 * 筛选条件卡外壳 —— 统一 Card(size=small,title=「筛选条件」)+ 重置按钮样板。
 * 表单字段保留在各页面组件内;本地状态/防抖见 hooks/useDebouncedFilters。
 */
const FilterCard: React.FC<FilterCardProps> = ({ onReset, extra, style, children }) => (
  <Card
    size="small"
    style={style ?? { marginBottom: 16 }}
    title="筛选条件"
    extra={
      <Space>
        {extra}
        <Button icon={<ClearOutlined />} size="small" onClick={onReset}>
          重置
        </Button>
      </Space>
    }
  >
    {children}
  </Card>
);

export default FilterCard;
