import React from 'react';
import { Button, Select, Space, Tooltip } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { REFRESH_INTERVAL_OPTIONS } from '../../hooks/useAutoRefresh';

interface RefreshControlsProps {
  /** 当前定时更新间隔(毫秒),null 表示关闭;下拉以 0 表示关闭 */
  interval: number | null;
  /** 修改定时更新间隔;传 null 关闭 */
  onIntervalChange: (ms: number | null) => void;
  /** 手动刷新回调 */
  onRefresh: () => void;
  /** 手动刷新是否进行中(刷新按钮 loading) */
  refreshing?: boolean;
  /** 最近一次刷新时间,尚未刷新过为 null 时不展示 */
  lastUpdated?: Date | null;
}

/**
 * 数据统计页面右上角刷新控件组:「定时更新」间隔选择 + 「刷新」按钮 + 上次更新时间。
 * 由 useAutoRefresh hook 驱动,页面对齐到标题行右侧。
 */
const RefreshControls: React.FC<RefreshControlsProps> = ({
  interval,
  onIntervalChange,
  onRefresh,
  refreshing = false,
  lastUpdated,
}) => (
  <Space size="middle">
    <span style={{ color: '#666', fontSize: 14 }}>定时更新:</span>
    <Select
      size="small"
      value={interval ?? 0}
      style={{ width: 90 }}
      options={REFRESH_INTERVAL_OPTIONS}
      onChange={(value: number) => onIntervalChange(value === 0 ? null : value)}
    />
    <Tooltip title="手动刷新数据">
      <Button size="small" icon={<ReloadOutlined />} loading={refreshing} onClick={onRefresh}>
        刷新
      </Button>
    </Tooltip>
    {lastUpdated && (
      <span style={{ color: '#999', fontSize: 12 }}>
        上次更新: {lastUpdated.toLocaleTimeString()}
      </span>
    )}
  </Space>
);

export default RefreshControls;
