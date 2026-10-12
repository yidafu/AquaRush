import React from 'react';
import { Button, Select, Space, Tooltip } from '@/ui';
import { ReloadOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { REFRESH_INTERVAL_OPTIONS } from '../../hooks/useAutoRefresh';

// 说明:#666 / #999 为本组件原有灰阶文字色,共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  label: {
    color: '#666',
    fontSize: '14px',
  },
  select: {
    width: '90px',
  },
  lastUpdated: {
    color: '#999',
    fontSize: '12px',
  },
});

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
    <span {...stylex.props(styles.label)}>定时更新:</span>
    <Select
      size="small"
      value={interval ?? 0}
      {...stylex.props(styles.select)}
      options={REFRESH_INTERVAL_OPTIONS}
      onChange={(value) => {
        // @/ui Select onChange 泛型为 V | V[] | undefined;本组件单选 number,收窄后回传
        const ms = typeof value === 'number' ? value : 0;
        onIntervalChange(ms === 0 ? null : ms);
      }}
    />
    <Tooltip title="手动刷新数据">
      <Button size="small" icon={<ReloadOutlined />} loading={refreshing} onClick={onRefresh}>
        刷新
      </Button>
    </Tooltip>
    {lastUpdated && (
      <span {...stylex.props(styles.lastUpdated)}>
        上次更新: {lastUpdated.toLocaleTimeString()}
      </span>
    )}
  </Space>
);

export default RefreshControls;
