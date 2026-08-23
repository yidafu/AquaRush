import { StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';

interface ListFooterStatusProps {
  /** 加载中(首屏/刷新/加载更多任一进行中) */
  loading: boolean;
  /** 是否已无更多数据 */
  noMore: boolean;
  /** 列表是否有数据(空列表时不展示「没有更多了」) */
  hasItems?: boolean;
}

/** 列表页脚状态：加载中… / 没有更多了。history-orders / reconciliation-records / admin-orders 共用。 */
export function ListFooterStatus({ loading, noMore, hasItems = true }: ListFooterStatusProps) {
  if (loading) {
    return (
      <ThemedText type="caption" themeColor="textTertiary" style={styles.footer}>
        加载中...
      </ThemedText>
    );
  }
  if (noMore && hasItems) {
    return (
      <ThemedText type="caption" themeColor="textTertiary" style={styles.footer}>
        没有更多了
      </ThemedText>
    );
  }
  return null;
}

const styles = StyleSheet.create({
  footer: {
    textAlign: 'center',
    paddingVertical: Spacing.three,
  },
});
