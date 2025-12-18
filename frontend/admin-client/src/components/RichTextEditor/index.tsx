import React, { useCallback, useRef, useState, useEffect } from 'react';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';

interface RichTextEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  height?: number;
  maxLength?: number;
  showCount?: boolean;
}

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value = '',
  onChange,
  placeholder = '请输入详情内容...',
  height = 300,
  maxLength,
  showCount = false
}) => {
  const quillRef = useRef<ReactQuill>(null);
  const [currentLength, setCurrentLength] = useState(0);

  // 计算字符数
  useEffect(() => {
    const updateLength = () => {
      if (quillRef.current) {
        const text = quillRef.current.getEditor().getText() || '';
        setCurrentLength(Math.max(0, text.length - 1)); // -1 因为 Quill 总是包含一个换行符
      }
    };

    // 使用 setTimeout 异步更新，避免同步 setState
    const timeoutId = setTimeout(updateLength, 0);
    return () => clearTimeout(timeoutId);
  }, [value]);

  const handleChange = useCallback((content: string, _delta: any, _source: string) => {
    if (quillRef.current) {
      const text = quillRef.current.getEditor().getText() || '';

      if (maxLength && text.length - 1 > maxLength) {
        return;
      }

      setCurrentLength(Math.max(0, text.length - 1));
      onChange?.(content);
    }
  }, [onChange, maxLength]);

  const modules = {
    toolbar: [
      [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
      ['bold', 'italic', 'underline', 'strike'],
      [{ 'color': [] }, { 'background': [] }],
      [{ 'script': 'sub' }, { 'script': 'super' }],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      [{ 'indent': '-1' }, { 'indent': '+1' }],
      [{ 'direction': 'rtl' }],
      [{ 'align': [] }],
      ['link', 'image', 'video'],
      ['blockquote', 'code-block'],
      ['clean']
    ],
  };

  const formats = [
    'header', 'font', 'size',
    'bold', 'italic', 'underline', 'strike', 'blockquote',
    'list', 'bullet', 'indent',
    'script', 'sub', 'super',
    'link', 'image', 'video',
    'color', 'background',
    'align', 'direction',
    'code-block'
  ];

  return (
    <div className="rich-text-editor">
      <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: '14px', color: '#666' }}>
          支持富文本编辑，可以设置标题、字体、颜色、列表、链接、图片等
        </span>
        {showCount && maxLength && (
          <span style={{ fontSize: '12px', color: currentLength > maxLength * 0.9 ? '#ff4d4f' : '#999' }}>
            {currentLength} / {maxLength}
          </span>
        )}
      </div>

      <div style={{
        border: '1px solid #d9d9d9',
        borderRadius: '6px',
        overflow: 'hidden'
      }}>
        <ReactQuill
          ref={quillRef}
          theme="snow"
          value={value}
          onChange={handleChange}
          modules={modules}
          formats={formats}
          placeholder={placeholder}
          style={{
            height,
            fontSize: '14px'
          }}
        />
      </div>

      {maxLength && (
        <div style={{
          marginTop: 8,
          fontSize: '12px',
          color: currentLength > maxLength * 0.9 ? '#ff4d4f' : '#999'
        }}>
          {currentLength} / {maxLength} 字符
          {currentLength > maxLength * 0.9 && '（接近字数限制）'}
        </div>
      )}
    </div>
  );
};

// 简化版本的富文本编辑器，使用更简单的工具栏
export const SimpleRichTextEditor: React.FC<RichTextEditorProps> = ({
  value = '',
  onChange,
  placeholder = '请输入内容...',
  maxLength,
  showCount = false
}) => {
  const quillRef = useRef<ReactQuill>(null);
  const [currentLength, setCurrentLength] = useState(0);

  useEffect(() => {
    const updateLength = () => {
      if (quillRef.current) {
        const text = quillRef.current.getEditor().getText() || '';
        setCurrentLength(Math.max(0, text.length - 1));
      }
    };

    const timeoutId = setTimeout(updateLength, 0);
    return () => clearTimeout(timeoutId);
  }, [value]);

  const handleChange = useCallback((content: string, _delta: any, _source: string) => {
    if (quillRef.current) {
      const text = quillRef.current.getEditor().getText() || '';

      if (maxLength && text.length - 1 > maxLength) {
        return;
      }

      setCurrentLength(Math.max(0, text.length - 1));
      onChange?.(content);
    }
  }, [onChange, maxLength]);

  // 简化的工具栏配置
  const modules = {
    toolbar: [
      [{ 'header': [1, 2, 3, false] }],
      ['bold', 'italic', 'underline'],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      ['link', 'clean'],
      [{ 'color': [] }]
    ],
  };

  const formats = [
    'header',
    'bold', 'italic', 'underline',
    'list', 'bullet',
    'link',
    'color'
  ];

  return (
    <div className="simple-rich-text-editor">
      <div style={{ marginBottom: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: '13px', color: '#666' }}>
          支持基础格式：标题、加粗、斜体、颜色、列表和链接
        </span>
        {showCount && maxLength && (
          <span style={{ fontSize: '12px', color: currentLength > maxLength * 0.9 ? '#ff4d4f' : '#999' }}>
            {currentLength} / {maxLength}
          </span>
        )}
      </div>

      <div style={{
        border: '1px solid #d9d9d9',
        borderRadius: '4px',
        overflow: 'hidden'
      }}>
        <ReactQuill
          ref={quillRef}
          theme="snow"
          value={value}
          onChange={handleChange}
          modules={modules}
          formats={formats}
          placeholder={placeholder}
          style={{
            height: 180,
            fontSize: '14px'
          }}
        />
      </div>

      {maxLength && (
        <div style={{
          marginTop: 4,
          fontSize: '12px',
          color: currentLength > maxLength * 0.9 ? '#ff4d4f' : '#999'
        }}>
          {currentLength} / {maxLength} 字符
          {currentLength > maxLength * 0.9 && '（接近字数限制）'}
        </div>
      )}
    </div>
  );
};