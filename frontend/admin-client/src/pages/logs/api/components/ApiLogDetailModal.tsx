import React from 'react';
import { Descriptions, Button } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { ApiLogItem } from '../../../../services/logs';
import JsonViewer from '../../../../components/JsonViewer';
import DetailModal from '@/components/DetailModal';
import { copyText } from '@/utils/clipboard';

export interface ApiLogDetailModalProps {
  log: ApiLogItem | null;
  open: boolean;
  onClose: () => void;
}

/** API 日志详情弹窗(logs/api/index.tsx 拆分;外壳复用 DetailModal) */
const ApiLogDetailModal: React.FC<ApiLogDetailModalProps> = ({ log, open, onClose }) => (
  <DetailModal title="请求详情" open={open} onClose={onClose} width={800}>
    {log && (
      <Descriptions column={2} bordered size="small">
        <Descriptions.Item label="请求类型">{log.requestType}</Descriptions.Item>
        <Descriptions.Item label="方法">{log.method}</Descriptions.Item>
        <Descriptions.Item label="URI" span={2}>{log.uri}</Descriptions.Item>
        <Descriptions.Item label="Query" span={2}>
          {log.query ? (
            <div>
              <pre style={{ maxHeight: 150, overflow: 'auto', fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace' }}>
                {log.query}
              </pre>
              <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(log.query!)}>
                复制 Query
              </Button>
            </div>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="状态码">{log.responseStatus}</Descriptions.Item>
        <Descriptions.Item label="耗时">{log.durationMs}ms</Descriptions.Item>
        <Descriptions.Item label="用户ID">{log.userId}</Descriptions.Item>
        <Descriptions.Item label="用户名">{log.username}</Descriptions.Item>
        <Descriptions.Item label="IP">{log.ipAddress}</Descriptions.Item>
        <Descriptions.Item label="UserAgent" span={2}>{log.userAgent}</Descriptions.Item>
        <Descriptions.Item label="关联ID">{log.correlationId}</Descriptions.Item>
        <Descriptions.Item label="操作类型">{log.operationType}</Descriptions.Item>
        <Descriptions.Item label="操作名称">{log.operationName}</Descriptions.Item>
        <Descriptions.Item label="请求体" span={2}>
          {log.requestBody ? (
            <JsonViewer value={log.requestBody} maxHeight={260} />
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="错误信息" span={2}>
          {log.errorMessage ? (
            <div>
              <pre style={{ maxHeight: 100, overflow: 'auto', fontSize: 12, color: 'red', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {log.errorMessage}
              </pre>
              <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(log.errorMessage!)}>
                复制错误信息
              </Button>
            </div>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="时间">{new Date(log.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
      </Descriptions>
    )}
  </DetailModal>
);

export default ApiLogDetailModal;
