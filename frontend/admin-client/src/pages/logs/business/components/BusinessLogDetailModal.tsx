import React from 'react';
import { Descriptions, Tag, Button } from '@/ui';
import { CopyOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import type { BusinessLogVo } from '@aquarush/common';
import JsonViewer from '../../../../components/JsonViewer';
import { getLogLevelInfo } from './constants';
import DetailModal from '@/components/DetailModal';
import { copyText } from '@/utils/clipboard';

// 消息 / 堆栈的等宽预格式化块(原内联 style 迁移)
const styles = stylex.create({
  messagePre: {
    maxHeight: '150px',
    overflow: 'auto',
    fontSize: '12px',
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
  },
  stackPre: {
    maxHeight: '200px',
    overflow: 'auto',
    fontSize: '12px',
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
  },
});

export interface BusinessLogDetailModalProps {
  log: BusinessLogVo | null;
  open: boolean;
  onClose: () => void;
}

// 消息是否以 JSON 对象/数组开头(用于选择 JsonViewer 或纯文本展示)
const looksLikeJson = (text?: string): boolean => {
  if (!text) return false;
  const trimmed = text.trim();
  return trimmed.startsWith('{') || trimmed.startsWith('[');
};

/** 业务日志详情弹窗(logs/business/index.tsx 拆分;外壳复用 DetailModal) */
const BusinessLogDetailModal: React.FC<BusinessLogDetailModalProps> = ({ log, open, onClose }) => (
  <DetailModal title="日志详情" open={open} onClose={onClose} width={800}>
    {log && (
      <Descriptions column={2} bordered size="small">
        <Descriptions.Item label="日志级别">
          <Tag color={getLogLevelInfo(log.level).color}>{log.level}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="时间">{log.createdAt ? new Date(log.createdAt).toLocaleString('zh-CN') : '-'}</Descriptions.Item>
        <Descriptions.Item label="Logger">{log.loggerName || '-'}</Descriptions.Item>
        <Descriptions.Item label="关联ID">{log.correlationId || '-'}</Descriptions.Item>
        <Descriptions.Item label="用户ID">{log.userId || '-'}</Descriptions.Item>
        <Descriptions.Item label="用户名">{log.username || '-'}</Descriptions.Item>
        <Descriptions.Item label="消息" span={2}>
          {log.message ? (
            looksLikeJson(log.message) ? (
              <JsonViewer value={log.message} maxHeight={220} />
            ) : (
              <pre {...stylex.props(styles.messagePre)}>
                {log.message}
              </pre>
            )
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="堆栈跟踪" span={2}>
          {log.stackTrace ? (
            <div>
              <pre {...stylex.props(styles.stackPre)}>
                {log.stackTrace}
              </pre>
              <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(log.stackTrace!)}>
                复制堆栈
              </Button>
            </div>
          ) : (
            '-'
          )}
        </Descriptions.Item>
      </Descriptions>
    )}
  </DetailModal>
);

export default BusinessLogDetailModal;
