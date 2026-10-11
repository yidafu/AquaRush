import React, { useCallback, useRef, useState, useEffect } from 'react';
import ReactQuill from 'react-quill-new';
import 'react-quill-new/dist/quill.snow.css';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// 局部色值 #666 / #999 / #d9d9d9 为原有灰阶文字与边框色,共享 token 无对应值,保持原样;
// 字数预警色依赖 currentLength state,保留 style={{ color }} 动态注入
const styles = stylex.create({
  header: {
    marginBottom: '8px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  hint: {
    fontSize: '14px',
    color: '#666',
  },
  count: {
    fontSize: '12px',
  },
  frame: {
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: '#d9d9d9',
    borderRadius: '6px',
    overflow: 'hidden',
  },
  editor: {
    fontSize: '14px',
  },
  countBlock: {
    marginTop: '8px',
    fontSize: '12px',
  },
  simpleHeader: {
    marginBottom: '4px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  simpleHint: {
    fontSize: '13px',
    color: '#666',
  },
  simpleFrame: {
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: '#d9d9d9',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  simpleEditor: {
    height: '180px',
    fontSize: '14px',
  },
  simpleCountBlock: {
    marginTop: '4px',
    fontSize: '12px',
  },
});

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
    <div>
      <div {...stylex.props(styles.header)}>
        <span {...stylex.props(styles.hint)}>
          支持富文本编辑，可以设置标题、字体、颜色、列表、链接、图片等
        </span>
        {showCount && maxLength && (
          <span
            {...stylex.props(styles.count)}
            style={{ color: currentLength > maxLength * 0.9 ? tokens.error : '#999' }}
          >
            {currentLength} / {maxLength}
          </span>
        )}
      </div>

      <div {...stylex.props(styles.frame)}>
        <ReactQuill
          ref={quillRef}
          theme="snow"
          value={value}
          onChange={handleChange}
          modules={modules}
          formats={formats}
          placeholder={placeholder}
          {...stylex.props(styles.editor)}
          style={{ height }}
        />
      </div>

      {maxLength && (
        <div
          {...stylex.props(styles.countBlock)}
          style={{ color: currentLength > maxLength * 0.9 ? tokens.error : '#999' }}
        >
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
    <div>
      <div {...stylex.props(styles.simpleHeader)}>
        <span {...stylex.props(styles.simpleHint)}>
          支持基础格式：标题、加粗、斜体、颜色、列表和链接
        </span>
        {showCount && maxLength && (
          <span
            {...stylex.props(styles.count)}
            style={{ color: currentLength > maxLength * 0.9 ? tokens.error : '#999' }}
          >
            {currentLength} / {maxLength}
          </span>
        )}
      </div>

      <div {...stylex.props(styles.simpleFrame)}>
        <ReactQuill
          ref={quillRef}
          theme="snow"
          value={value}
          onChange={handleChange}
          modules={modules}
          formats={formats}
          placeholder={placeholder}
          {...stylex.props(styles.simpleEditor)}
        />
      </div>

      {maxLength && (
        <div
          {...stylex.props(styles.simpleCountBlock)}
          style={{ color: currentLength > maxLength * 0.9 ? tokens.error : '#999' }}
        >
          {currentLength} / {maxLength} 字符
          {currentLength > maxLength * 0.9 && '（接近字数限制）'}
        </div>
      )}
    </div>
  );
};