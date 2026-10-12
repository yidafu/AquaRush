/**
 * 账号管理列表页纯辅助(users/accounts/index.tsx 拆分)。
 */

/** 「用户类型」过滤参数(2026-08 设计调整:替代原角色 Tab,无 Tab 切换) */
export function roleFilterOptionsFor(isSuperAdmin: boolean): { value: string; label: string }[] {
  if (isSuperAdmin) {
    return [
      { value: 'ALL', label: '全部' },
      { value: 'SUPER_ADMIN', label: '超级管理员' },
      { value: 'ADMIN', label: '管理员' },
      { value: 'DELIVERY_WORKER', label: '送水员' },
    ];
  }
  return [
    { value: 'ALL', label: '全部' },
    { value: 'ADMIN', label: '兼送水管理员' },
    { value: 'DELIVERY_WORKER', label: '送水员' },
  ];
}
