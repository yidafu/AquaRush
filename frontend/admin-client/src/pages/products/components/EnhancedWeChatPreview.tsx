import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Alert, Button, Tooltip, Space } from '@/ui';
import { ReloadOutlined, FullscreenOutlined, MobileOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// 说明:#f5f5f5 / #d9d9d9 / #666 / #999 为预览区局部色值,token 无对应值,
// 保留字面量。原 .wechat-preview-wrapper 全局无定义(死类),已删除。
// frame 的 height 依赖 height prop,保留 style={{ }} 动态注入。
const styles = stylex.create({
  controlsBar: {
    marginBottom: '12px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  previewLabel: {
    fontSize: '14px',
    color: '#666',
  },
  frame: {
    position: 'relative',
    width: '100%',
    backgroundColor: '#f5f5f5',
    borderRadius: '8px',
    overflow: 'hidden',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: '#d9d9d9',
  },
  errorDesc: {
    fontSize: '12px',
    color: '#666',
    marginTop: '8px',
  },
  errorAlert: {
    marginTop: '20px',
    marginRight: '20px',
    marginBottom: '20px',
    marginLeft: '20px',
  },
  iframe: {
    backgroundColor: tokens.card,
    borderRadius: '8px',
  },
  footer: {
    marginTop: '8px',
    fontSize: '12px',
    color: '#999',
    textAlign: 'center',
    display: 'flex',
    justifyContent: 'space-between',
  },
});

interface EnhancedWeChatPreviewProps {
  productId: string;
  className?: string;
  showControls?: boolean;
  height?: number;
  productName?: string;
  onProductUpdate?: () => void;
}

export const EnhancedWeChatPreview: React.FC<EnhancedWeChatPreviewProps> = ({
  productId,
  className,
  showControls = true,
  height = 600,
  productName = '',
  onProductUpdate
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Generate preview URL when productId changes
  const previewUrl = useMemo(() => {
    if (!productId) return '';

    // Use local H5 files for admin preview
    return `/weapp/index.html#/pages/product-detail/index?id=${productId}&preview=true&from=admin`;
  }, [productId]);

  useEffect(() => {
    if (!productId) return;
    setLoading(true);
    setError(null);
  }, [productId]);

  const handleReload = () => {
    setLoading(true);
    setError(null);
    if (iframeRef.current) {
      // Force reload by setting src to empty then back to the URL
      const currentSrc = iframeRef.current.src;
      iframeRef.current.src = '';
      setTimeout(() => {
        if (iframeRef.current) {
          iframeRef.current.src = currentSrc;
        }
      }, 100);
    }
  };

  const handleFullscreen = () => {
    if (iframeRef.current?.requestFullscreen) {
      iframeRef.current.requestFullscreen();
    }
  };

  const handleIframeLoad = () => {
    setLoading(false);
    setError(null);
  };

  const handleIframeError = () => {
    setLoading(false);
    setError('预览加载失败，请检查H5文件是否已编译到 /weapp/ 目录');
  };

  return (
    <div className={className}>
      {showControls && (
        <div {...stylex.props(styles.controlsBar)}>
          <Space>
            <MobileOutlined />
            <span {...stylex.props(styles.previewLabel)}>
              小程序预览 {productName && `- ${productName}`}
            </span>
          </Space>
          <Space>
            <Tooltip title="刷新预览">
              <Button
                icon={<ReloadOutlined />}
                onClick={handleReload}
                size="small"
                loading={loading}
              >
                刷新
              </Button>
            </Tooltip>
            {onProductUpdate && (
              <Tooltip title="保存并刷新">
                <Button
                  icon={<ReloadOutlined />}
                  onClick={onProductUpdate}
                  size="small"
                >
                  保存并刷新
                </Button>
              </Tooltip>
            )}
            <Tooltip title="全屏预览">
              <Button
                icon={<FullscreenOutlined />}
                onClick={handleFullscreen}
                size="small"
              >
                全屏
              </Button>
            </Tooltip>
          </Space>
        </div>
      )}

      <div
        {...stylex.props(styles.frame)}
        style={{ height: `${height}px` }}
      >

          {error ? (
            <Alert
              message="预览加载失败"
              description={
                <div>
                  <p>{error}</p>
                  <p {...stylex.props(styles.errorDesc)}>
                    请运行 'npm run build:h5:admin' 编译H5文件到 /public/weapp/ 目录
                  </p>
                </div>
              }
              type="error"
              showIcon
              {...stylex.props(styles.errorAlert)}
              action={
                <Button size="small" onClick={handleReload}>
                  重试
                </Button>
              }
            />
          ) : (
            <iframe
              ref={iframeRef}
              src={previewUrl}
              width="100%"
              height="100%"
              frameBorder="0"
              onLoad={handleIframeLoad}
              onError={handleIframeError}
              {...stylex.props(styles.iframe)}
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
              title="微信小程序预览"
            />
          )}
      </div>

      <div {...stylex.props(styles.footer)}>
        <span>商品ID: {productId}</span>
        <span>本地H5预览</span>
      </div>
    </div>
  );
};
