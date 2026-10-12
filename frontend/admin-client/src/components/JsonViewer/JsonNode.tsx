import React, { useMemo, useState } from 'react';
import { Button, Text } from '@/ui';
import { DownOutlined, RightOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';

export type CollapseMode = 'default' | 'expandAll' | 'collapseAll';

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
});

export function isObjectLike(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

export function renderPrimitive(value: unknown): React.ReactNode {
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

export default JsonNode;
