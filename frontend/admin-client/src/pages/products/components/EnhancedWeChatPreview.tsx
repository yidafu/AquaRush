import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Spin, Alert, Button, Tooltip, Space } from 'antd';
import { ReloadOutlined, FullscreenOutlined, MobileOutlined } from '@ant-design/icons';

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
    <div className={`wechat-preview-wrapper ${className || ''}`}>
      {showControls && (
        <div style={{
          marginBottom: 12,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <Space>
            <MobileOutlined />
            <span style={{ fontSize: '14px', color: '#666' }}>
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
        style={{
          position: 'relative',
          width: '100%',
          height: `${height}px`,
          background: '#f5f5f5',
          borderRadius: '8px',
          overflow: 'hidden',
          border: '1px solid #d9d9d9'
        }}
      >

          {error ? (
            <Alert
              message="预览加载失败"
              description={
                <div>
                  <p>{error}</p>
                  <p style={{ fontSize: '12px', color: '#666', marginTop: 8 }}>
                    请运行 'npm run build:h5:admin' 编译H5文件到 /public/weapp/ 目录
                  </p>
                </div>
              }
              type="error"
              showIcon
              action={
                <Button size="small" onClick={handleReload}>
                  重试
                </Button>
              }
              style={{ margin: 20 }}
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
              style={{
                background: '#fff',
                borderRadius: '8px'
              }}
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
              title="微信小程序预览"
            />
          )}
      </div>

      <div style={{
        marginTop: 8,
        fontSize: '12px',
        color: '#999',
        textAlign: 'center',
        display: 'flex',
        justifyContent: 'space-between'
      }}>
        <span>商品ID: {productId}</span>
        <span>本地H5预览</span>
      </div>
    </div>
  );
};
