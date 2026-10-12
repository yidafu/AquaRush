import React, { useMemo, useState } from 'react';
import { Button, Space, message } from '@aquarush/admin-ui';
import {
  CopyOutlined,
  CheckOutlined,
  MinusSquareOutlined,
  PlusSquareOutlined,
} from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import JsonNode, { renderPrimitive } from './JsonNode';
import type { CollapseMode } from './JsonNode';

/**
 * 只读 JSON 查看器
 *
 * - 入参 value 支持对象或 JSON 字符串;解析失败回退原始文本展示;
 * - 对象/数组可折叠,支持「展开全部 / 收起全部」;
 * - 语法着色(键蓝 / 字符串绿 / 数字橙 / 布尔与 null 紫);
 * - 一键复制(复制格式化后的 JSON)。
 *
 * 用于日志详情等场景替代裸 <pre> 文本,不引入任何第三方依赖。
 */
export interface JsonViewerProps {
  /** JSON 字符串或对象;null/undefined 显示 '-' */
  value?: string | object | null;
  /** 容器最大高度(px),超出滚动 */
  maxHeight?: number;
  /** 是否显示复制按钮,默认 true */
  copyable?: boolean;
  /** 默认展开层级(0=全部折叠,1=仅第一层展开),默认 1 */
  defaultExpandLevel?: number;
}

const styles = stylex.create({
  flatIndent: {
    paddingLeft: '0px',
  },
  toolbar: {
    marginBottom: '4px',
  },
  viewerBox: {
    overflow: 'auto',
    backgroundColor: '#fafafa',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: '#f0f0f0',
    borderRadius: '6px',
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '12px',
    paddingRight: '12px',
    fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
    fontSize: '12px',
  },
  rawPre: {
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    overflow: 'auto',
    fontSize: '12px',
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
    fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
    color: '#333333',
  },
});

const JsonViewer: React.FC<JsonViewerProps> = ({
  value,
  maxHeight = 300,
  copyable = true,
  defaultExpandLevel = 1,
}) => {
  const [copied, setCopied] = useState(false);
  const [collapseMode, setCollapseMode] = useState<CollapseMode>('default');
  const [modeTick, setModeTick] = useState(0);

  // 解析:对象直接用;字符串尝试 parse;失败进入原始文本模式
  const parsed = useMemo<{ ok: boolean; data: unknown; raw: string }>(() => {
    if (value === null || value === undefined) {
      return { ok: false, data: undefined, raw: '' };
    }
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (!trimmed) return { ok: false, data: undefined, raw: '' };
      try {
        return { ok: true, data: JSON.parse(trimmed), raw: trimmed };
      } catch {
        return { ok: false, data: undefined, raw: value };
      }
    }
    return { ok: true, data: value, raw: JSON.stringify(value) };
  }, [value]);

  if (!parsed.ok) {
    return (
      <pre {...stylex.props(styles.rawPre)} style={{ maxHeight }}>
        {parsed.raw || '-'}
      </pre>
    );
  }

  const isCollapsible = parsed.data !== null && typeof parsed.data === 'object';

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(parsed.data, null, 2));
      setCopied(true);
      message.success('已复制 JSON');
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      message.error('复制失败');
    }
  };

  return (
    <div>
      <Space {...stylex.props(styles.toolbar)} size={4}>
        {isCollapsible && (
          <>
            <Button
              size="small"
              type="text"
              icon={<PlusSquareOutlined />}
              onClick={() => {
                setCollapseMode('expandAll');
                setModeTick(t => t + 1);
              }}
            >
              展开全部
            </Button>
            <Button
              size="small"
              type="text"
              icon={<MinusSquareOutlined />}
              onClick={() => {
                setCollapseMode('collapseAll');
                setModeTick(t => t + 1);
              }}
            >
              收起全部
            </Button>
          </>
        )}
        {copyable && (
          <Button
            size="small"
            type="text"
            icon={copied ? <CheckOutlined /> : <CopyOutlined />}
            onClick={handleCopy}
            style={{ color: copied ? tokens.success : undefined }}
          >
            {copied ? '已复制' : '复制'}
          </Button>
        )}
      </Space>
      <div {...stylex.props(styles.viewerBox)} style={{ maxHeight }}>
        {isCollapsible ? (
          <JsonNode
            key={modeTick}
            value={parsed.data}
            depth={0}
            defaultExpandLevel={defaultExpandLevel}
            collapseMode={collapseMode}
          />
        ) : (
          <div {...stylex.props(styles.flatIndent)}>{renderPrimitive(parsed.data)}</div>
        )}
      </div>
    </div>
  );
};

export default JsonViewer;
