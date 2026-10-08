import React, { useMemo, useState } from 'react';
import { Button, Space, Typography, message } from 'antd';
import {
  CopyOutlined,
  CheckOutlined,
  DownOutlined,
  RightOutlined,
  MinusSquareOutlined,
  PlusSquareOutlined,
} from '@ant-design/icons';

const { Text } = Typography;

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

type CollapseMode = 'default' | 'expandAll' | 'collapseAll';

interface JsonNodeProps {
  name?: string;
  value: unknown;
  depth: number;
  defaultExpandLevel: number;
  collapseMode: CollapseMode;
}

const COLORS = {
  key: '#0b5cab',
  string: '#22863a',
  number: '#b35900',
  boolean: '#6f42c1',
  null: '#6f42c1',
  punctuation: '#888888',
  raw: '#333333',
};

function isObjectLike(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function renderPrimitive(value: unknown): React.ReactNode {
  if (value === null) {
    return <Text style={{ color: COLORS.null }}>null</Text>;
  }
  switch (typeof value) {
    case 'string':
      return <Text style={{ color: COLORS.string }}>"{value}"</Text>;
    case 'number':
      return <Text style={{ color: COLORS.number }}>{String(value)}</Text>;
    case 'boolean':
      return <Text style={{ color: COLORS.boolean }}>{String(value)}</Text>;
    default:
      return <Text style={{ color: COLORS.raw }}>{String(value)}</Text>;
  }
}

const JsonNode: React.FC<JsonNodeProps> = ({ name, value, depth, defaultExpandLevel, collapseMode }) => {
  const isArray = Array.isArray(value);
  const isObject = isObjectLike(value);
  const collapsible = isArray || isObject;

  // 初始折叠状态:展开全部/收起全部模式下按模式初始化,否则按层级
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    if (collapseMode === 'expandAll') return false;
    if (collapseMode === 'collapseAll') return true;
    return depth >= defaultExpandLevel;
  });

  const entries = useMemo(() => {
    if (isArray) {
      return (value as unknown[]).map((item, index) => ({ key: String(index), item }));
    }
    if (isObject) {
      return Object.entries(value).map(([key, item]) => ({ key, item }));
    }
    return [];
  }, [value, isArray, isObject]);

  if (!collapsible) {
    return (
      <div style={{ paddingLeft: depth * 16 }}>
        {name !== undefined && <Text style={{ color: COLORS.key }}>{name}: </Text>}
        {renderPrimitive(value)}
      </div>
    );
  }

  const openBracket = isArray ? '[' : '{';
  const closeBracket = isArray ? ']' : '}';
  const summary = isArray ? `Array(${entries.length})` : `Object(${entries.length})`;

  return (
    <div>
      <div style={{ paddingLeft: depth * 16, display: 'flex', alignItems: 'center', gap: 4 }}>
        {name !== undefined && <Text style={{ color: COLORS.key }}>{name}: </Text>}
        <Button
          type="text"
          size="small"
          style={{ padding: 0, height: 'auto', lineHeight: 'inherit' }}
          icon={collapsed ? <RightOutlined style={{ fontSize: 10 }} /> : <DownOutlined style={{ fontSize: 10 }} />}
          onClick={() => setCollapsed(!collapsed)}
        />
        <Text style={{ color: COLORS.punctuation }}>{openBracket}</Text>
        {collapsed && (
          <>
            <Text type="secondary" style={{ fontSize: 12, color: COLORS.punctuation }}>
              {summary}
            </Text>
            <Text style={{ color: COLORS.punctuation }}>{closeBracket}</Text>
          </>
        )}
      </div>
      {!collapsed && (
        <>
          {entries.map(({ key, item }) => (
            <JsonNode
              key={key}
              name={isArray ? undefined : key}
              value={item}
              depth={depth + 1}
              defaultExpandLevel={defaultExpandLevel}
              collapseMode={collapseMode}
            />
          ))}
          <div style={{ paddingLeft: depth * 16 }}>
            <Text style={{ color: COLORS.punctuation }}>{closeBracket}</Text>
          </div>
        </>
      )}
    </div>
  );
};

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
      <pre
        style={{
          margin: 0,
          maxHeight,
          overflow: 'auto',
          fontSize: 12,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
          color: COLORS.raw,
        }}
      >
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
      <Space style={{ marginBottom: 4 }} size={4}>
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
            style={{ color: copied ? '#52c41a' : undefined }}
          >
            {copied ? '已复制' : '复制'}
          </Button>
        )}
      </Space>
      <div
        style={{
          maxHeight,
          overflow: 'auto',
          background: '#fafafa',
          border: '1px solid #f0f0f0',
          borderRadius: 6,
          padding: '8px 12px',
          fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
          fontSize: 12,
        }}
      >
        {isCollapsible ? (
          <JsonNode
            key={modeTick}
            value={parsed.data}
            depth={0}
            defaultExpandLevel={defaultExpandLevel}
            collapseMode={collapseMode}
          />
        ) : (
          <div style={{ paddingLeft: 0 }}>{renderPrimitive(parsed.data)}</div>
        )}
      </div>
    </div>
  );
};

export default JsonViewer;
