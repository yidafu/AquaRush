import React, { useState, useCallback } from "react";
import { Input, Button, Space, Collapse, message, Typography } from "antd";
import {
  FormatPainterOutlined,
  CopyOutlined,
  CheckOutlined,
} from "@ant-design/icons";

const { TextArea } = Input;
const { Panel } = Collapse;
const { Text } = Typography;

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
    <div className="json-editor">
      <div style={{ marginBottom: 12 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
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
          <Text type="secondary" style={{ fontSize: "12px" }}>
            {help}
          </Text>
        )}
      </div>

      {templates.length > 0 && (
        <Collapse size="small" style={{ marginBottom: 12 }}>
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
        style={{
          fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace',
          fontSize: "13px",
          borderColor: isValid ? undefined : "#ff4d4f",
        }}
      />

      {!isValid && (
        <div
          style={{
            marginTop: 8,
            padding: "8px 12px",
            background: "#fff2f0",
            border: "1px solid #ffccc7",
            borderRadius: "6px",
          }}
        >
          <Text type="danger" style={{ fontSize: "12px" }}>
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

// 特化的配送设置编辑器
export const DeliverySettingsEditor: React.FC<
  Omit<JsonEditorProps, "title" | "templates">
> = (props) => {
  const deliveryTemplates = [
    {
      name: "免费配送",
      value: {
        freeShipping: true,
        minOrder: 0,
      },
    },
    {
      name: "满额免运费",
      value: {
        freeShipping: false,
        minOrder: 50,
        shippingFee: 5,
      },
    },
    {
      name: "限定区域",
      value: {
        freeShipping: true,
        areas: ["北京", "上海", "广州"],
        excludeAreas: ["偏远地区"],
      },
    },
    {
      name: "定时配送",
      value: {
        freeShipping: false,
        timeSlots: ["09:00-12:00", "14:00-18:00"],
        advanceDays: 2,
      },
    },
  ];

  return (
    <JsonEditor
      {...props}
      title="配送设置编辑器"
      help="配置配送相关信息。支持设置免运费条件、配送区域、时间段等。"
      templates={deliveryTemplates}
      placeholder='例如：{"freeShipping": true, "minOrder": 50} 或 {"areas": ["北京", "上海"]}'
    />
  );
};
