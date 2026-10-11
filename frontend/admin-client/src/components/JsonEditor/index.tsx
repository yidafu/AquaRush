import React, { useState, useCallback } from "react";
import { Input, Button, Space, Collapse, message, Typography } from '@/ui';
import {
  FormatPainterOutlined,
  CopyOutlined,
  CheckOutlined,
} from '@/ui/icons';
import * as stylex from "@stylexjs/stylex";
import { tokens } from "@aquarush/common/theme/tokens.stylex";

const { TextArea } = Input;
const { Panel } = Collapse;
const { Text } = Typography;

// 局部色值 #fff2f0 / #ffccc7 为 antd 错误提示底与边框色,共享 token 无对应值,保持原样;
// 校验失败边框色依赖 isValid state,保留 style={{ borderColor }} 动态注入
const styles = stylex.create({
  header: {
    marginBottom: "12px",
  },
  titleRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  meta: {
    fontSize: "12px",
  },
  collapse: {
    marginBottom: "12px",
  },
  codeArea: {
    fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
    fontSize: "13px",
  },
  errorBox: {
    marginTop: "8px",
    paddingTop: "8px",
    paddingBottom: "8px",
    paddingLeft: "12px",
    paddingRight: "12px",
    backgroundColor: "#fff2f0",
    borderWidth: "1px",
    borderStyle: "solid",
    borderColor: "#ffccc7",
    borderRadius: "6px",
  },
});

interface JsonEditorProps {
  value?: any;
  onChange?: (value: any) => void;
  placeholder?: string;
  title?: string;
  help?: string;
  templates?: Array<{ name: string; value: any }>;
}

export const JsonEditor: React.FC<JsonEditorProps> = ({
  value,
  onChange,
  placeholder = "请输入JSON格式数据",
  title = "JSON编辑器",
  help,
  templates = [],
}) => {
  const [isValid, setIsValid] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [copied, setCopied] = useState(false);

  // Ensure value is a string
  const safeValue = typeof value === 'string' ? value : (value ? JSON.stringify(value, null, 2) : "");

  const validateJson = useCallback((str: string): boolean => {
    if (!str.trim()) {
      setIsValid(true);
      setErrorMessage("");
      return true;
    }

    try {
      JSON.parse(str);
      setIsValid(true);
      setErrorMessage("");
      return true;
    } catch (error) {
      setIsValid(false);
      setErrorMessage(error instanceof Error ? error.message : "JSON格式错误");
      return false;
    }
  }, []);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const val = e.target.value;

      // 验证JSON格式
      if (val.trim()) {
        const isValidJson = validateJson(val);
        if (isValidJson) {
          try {
            const parsed = JSON.parse(val);
            onChange?.(parsed);
          } catch {
            // 解析失败时返回 null，表示无效数据
            onChange?.(null);
          }
        } else {
          // JSON 验证失败时返回 null
          onChange?.(null);
        }
      } else {
        setIsValid(true);
        setErrorMessage("");
        onChange?.(null);
      }
    },
    [onChange, validateJson],
  );

  const formatJson = () => {
    if (!safeValue.trim()) {
      message.warning("没有内容需要格式化");
      return;
    }

    try {
      const parsed = JSON.parse(safeValue);
      onChange?.(parsed);
      message.success("JSON格式化成功");
    } catch (error) {
      message.error("JSON格式错误，无法格式化");
    }
  };

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(safeValue);
      setCopied(true);
      message.success("已复制到剪贴板");
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      message.error("复制失败");
    }
  };

  const useTemplate = (templateValue: any) => {
    onChange?.(templateValue);
    message.success("已应用模板");
  };

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <div {...stylex.props(styles.titleRow)}>
          <Text strong>{title}</Text>
          <Space>
            <Button
              size="small"
              icon={<FormatPainterOutlined />}
              onClick={formatJson}
              disabled={!safeValue.trim()}
            >
              格式化
            </Button>
            <Button
              size="small"
              icon={copied ? <CheckOutlined /> : <CopyOutlined />}
              onClick={copyToClipboard}
              disabled={!safeValue.trim()}
              type={copied ? "primary" : "default"}
            >
              {copied ? "已复制" : "复制"}
            </Button>
          </Space>
        </div>
        {help && (
          <Text type="secondary" {...stylex.props(styles.meta)}>
            {help}
          </Text>
        )}
      </div>

      {templates.length > 0 && (
        <Collapse size="small" {...stylex.props(styles.collapse)}>
          <Panel header="快速模板" key="templates">
            <Space wrap>
              {templates.map((template, index) => (
                <Button
                  key={index}
                  size="small"
                  onClick={() => useTemplate(template.value)}
                >
                  {template.name}
                </Button>
              ))}
            </Space>
          </Panel>
        </Collapse>
      )}

      <TextArea
        value={safeValue}
        onChange={handleChange}
        placeholder={placeholder}
        rows={6}
        {...stylex.props(styles.codeArea)}
        style={{ borderColor: isValid ? undefined : tokens.error }}
      />

      {!isValid && (
        <div {...stylex.props(styles.errorBox)}>
          <Text type="danger" {...stylex.props(styles.meta)}>
            JSON格式错误: {errorMessage}
          </Text>
        </div>
      )}

          </div>
  );
};

// 特化的标签编辑器
export const TagsEditor: React.FC<
  Omit<JsonEditorProps, "title" | "templates">
> = (props) => {
  const tagTemplates = [
    { name: "基础标签", value: ["矿泉水", "纯净水"] },
    { name: "高端产品", value: ["高端", "进口", "优质"] },
    { name: "健康饮品", value: ["健康", "无糖", "天然"] },
    {
      name: "促销标签",
      value: { promotion: true, discount: "10%", label: "限时特惠" },
    },
  ];

  return (
    <JsonEditor
      {...props}
      title="标签编辑器"
      help='支持数组格式或对象格式。数组示例：["标签1", "标签2"]；对象示例：{"key": "value"}'
      templates={tagTemplates}
      placeholder='例如：["矿泉水", "纯净水"] 或 {"类型": "天然矿泉水", "等级": "优级"}'
    />
  );
};
