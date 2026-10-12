import React from 'react';
import { Button } from '@/ui';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// 静态内联样式迁 StyleX;#666 为页面局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  pageTitle: {
    // .page-title(index.css 自研类)已删除,内联进组件(原 margin: 0 拆 longhand,内联 marginBottom: 8)
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '8px',
    marginLeft: '0px',
    fontSize: tokens.fsPage,
    lineHeight: tokens.lhPage,
    fontWeight: 600,
  },
  subtitleRow: {
    marginBottom: '16px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  subtitleText: {
    color: '#666',
  },
});

export interface PageHeaderProps {
  canExecute: boolean;
  onManualExecute: () => void;
}

/** 页面标题 + 对账口径副标题 + 手动对账入口(由 index.tsx 拆分) */
const PageHeader: React.FC<PageHeaderProps> = ({ canExecute, onManualExecute }) => (
  <>
    <h1 {...stylex.props(styles.pageTitle)}>
      微信支付对账
    </h1>
    <div {...stylex.props(styles.subtitleRow)}>
      <div {...stylex.props(styles.subtitleText)}>
        每日凌晨自动对账（支付 02:00 / 退款 02:05）｜ 以微信官方 T-1 账单为基准逐笔比对 ｜ 差异人工处置留痕（R-PAY-04）
      </div>
      {canExecute && (
        <Button
          type="primary"
          onClick={onManualExecute}
        >
          手动对账
        </Button>
      )}
    </div>
  </>
);

export default PageHeader;
