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
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

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

// JSON 语法着色(键蓝 / 字符串绿 / 数字橙 / 布尔与 null 紫)——
// 本组件局部色值,共享 token 无对应值,保持原样(报告 (a));
// maxHeight / paddingLeft(depth) 依赖 props,保留 style={{ }} 动态注入
const styles = stylex.create({
  keyColor: {
    color: '#0b5cab',
  },
  stringColor: {
    color: '#22863a',
  },
  numberColor: {
    color: '#b35900',
  },
  booleanColor: {
    color: '#6f42c1',
  },
  nullColor: {
    color: '#6f42c1',
  },
  punctuationColor: {
    color: '#888888',
  },
  rawColor: {
    color: '#333333',
  },
  nodeHeader: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '4px',
    rowGap: '4px',
  },
  toggleBtn: {
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    height: 'auto',
    lineHeight: 'inherit',
  },
  toggleIcon: {
    fontSize: '10px',
  },
  summaryText: {
    fontSize: '12px',
    color: '#888888',
  },
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

function isObjectLike(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function renderPrimitive(value: unknown): React.ReactNode {
  if (value === null) {
    return <Text {...stylex.props(styles.nullColor)}>null</Text>;
  }
  switch (typeof value) {
    case 'string':
      return <Text {...stylex.props(styles.stringColor)}>"{value}"</Text>;
    case 'number':
      return <Text {...stylex.props(styles.numberColor)}>{String(value)}</Text>;
    case 'boolean':
      return <Text {...stylex.props(styles.booleanColor)}>{String(value)}</Text>;
    default:
      return <Text {...stylex.props(styles.rawColor)}>{String(value)}</Text>;
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
        {name !== undefined && <Text {...stylex.props(styles.keyColor)}>{name}: </Text>}
        {renderPrimitive(value)}
      </div>
    );
  }

  const openBracket = isArray ? '[' : '{';
  const closeBracket = isArray ? ']' : '}';
  const summary = isArray ? `Array(${entries.length})` : `Object(${entries.length})`;

  return (
    <div>
      <div {...stylex.props(styles.nodeHeader)} style={{ paddingLeft: depth * 16 }}>
        {name !== undefined && <Text {...stylex.props(styles.keyColor)}>{name}: </Text>}
        <Button
          type="text"
          size="small"
          {...stylex.props(styles.toggleBtn)}
          icon={
            collapsed ? (
              <RightOutlined {...stylex.props(styles.toggleIcon)} />
            ) : (
              <DownOutlined {...stylex.props(styles.toggleIcon)} />
            )
          }
          onClick={() => setCollapsed(!collapsed)}
        />
        <Text {...stylex.props(styles.punctuationColor)}>{openBracket}</Text>
        {collapsed && (
          <>
            <Text type="secondary" {...stylex.props(styles.summaryText)}>
              {summary}
            </Text>
            <Text {...stylex.props(styles.punctuationColor)}>{closeBracket}</Text>
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
            <Text {...stylex.props(styles.punctuationColor)}>{closeBracket}</Text>
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
