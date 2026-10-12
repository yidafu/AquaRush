import React from 'react';
import { Tag, Text, Paragraph, Empty, Spin } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { formatDateTime } from '@aquarush/common/utils';
import { TIMELINE_COLORS, LOG_LEVEL_COLOR, getOperationConfig, parseOperationFromMessage } from './OperationConfig';

export { OPERATION_CONFIG, DOMAIN_OPERATIONS } from './OperationConfig';

const styles = stylex.create({
  loadingBox: {
    textAlign: 'center',
    paddingTop: '48px',
    paddingBottom: '48px',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
  empty: {
    paddingTop: '48px',
    paddingBottom: '48px',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
  item: {
    paddingBottom: '8px',
  },
  itemHeader: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    marginBottom: '4px',
    flexWrap: 'wrap',
  },
  message: {
    marginBottom: '4px',
    fontSize: '13px',
  },
  meta: {
    fontSize: '12px',
  },
  timeRow: {
    marginTop: '4px',
  },
  // —— 垂直时间线(替代 antd Timeline mode="left";@aquarush/admin-ui 无 Timeline)——
  timeline: {
    listStyleType: 'none',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
  },
  timelineItem: {
    position: 'relative',
  },
  timelineHead: {
    position: 'absolute',
    top: '3px',
    left: '0px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    fontSize: '14px',
    // 动态:图标颜色由条目 color 决定,经 style={{}} 注入
  },
  timelineTail: {
    position: 'absolute',
    top: '19px',
    bottom: '0px',
    left: '7px',
    width: '2px',
    backgroundColor: tokens.border,
  },
  timelineContent: {
    paddingTop: '0px',
    paddingBottom: '12px',
    paddingLeft: '24px',
    paddingRight: '0px',
  },
});

export interface BusinessLogItem {
  id: string;
  correlationId?: string;
  level: 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  loggerName?: string;
  operation?: string;
  message?: string;
  stackTrace?: string;
  userId?: number;
  username?: string;
  createdAt: string;
}

interface BusinessLogTimelineProps {
  logs: BusinessLogItem[];
  loading?: boolean;
}

/**
 * 业务日志时间线视图(F-LOG-003):以用户为视角展示业务操作记录,恒 createdAt 倒序。
 * 条目含 级别 Tag、操作类型(图标+色+文案)、用户 Tag、消息(省略)、关联ID、本地化时间。
 */
const BusinessLogTimeline: React.FC<BusinessLogTimelineProps> = ({ logs, loading }) => {
  if (loading) {
    return (
      <div {...stylex.props(styles.loadingBox)}>
        <Spin />
      </div>
    );
  }

  if (logs.length === 0) {
    return <Empty description="暂无操作记录" {...stylex.props(styles.empty)} />;
  }

  return (
    <ol {...stylex.props(styles.timeline)}>
      {logs.map((log) => {
        const operation = log.operation || parseOperationFromMessage(log.message);
        const config = getOperationConfig(operation);
        const userLabel = log.username || (log.userId != null ? String(log.userId) : '-');
        return (
          <li key={log.id} {...stylex.props(styles.timelineItem)}>
            {/* 动态:图标颜色取决于条目 color */}
            <span
              {...stylex.props(styles.timelineHead)}
              style={{ color: TIMELINE_COLORS[config.color] ?? TIMELINE_COLORS.gray }}
            >
              {config.icon}
            </span>
            <div {...stylex.props(styles.timelineTail)} />
            <div {...stylex.props(styles.timelineContent)}>
              <div {...stylex.props(styles.item)}>
                <div {...stylex.props(styles.itemHeader)}>
                  <Text strong>{config.label}</Text>
                  <Tag color={LOG_LEVEL_COLOR[log.level] || 'default'}>{log.level}</Tag>
                  <Tag color="blue">{userLabel}</Tag>
                </div>
                {log.message && (
                  <Paragraph
                    type="secondary"
                    ellipsis={{ rows: 2 }}
                    {...stylex.props(styles.message)}
                  >
                    {log.message}
                  </Paragraph>
                )}
                {log.correlationId && (
                  <Text type="secondary" {...stylex.props(styles.meta)}>
                    关联ID: {log.correlationId}
                  </Text>
                )}
                <div {...stylex.props(styles.timeRow)}>
                  <Text type="secondary" {...stylex.props(styles.meta)}>
                    {formatDateTime(log.createdAt)}
                  </Text>
                </div>
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
};

export default BusinessLogTimeline;
