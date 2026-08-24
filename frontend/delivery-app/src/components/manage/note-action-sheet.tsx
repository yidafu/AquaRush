import { useCallback, useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { BottomSheet } from '@/components/bottom-sheet';
import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';

interface NoteActionSheetProps {
  visible: boolean;
  title: string;
  /** 弹层说明(如待处理记录摘要) */
  description?: string;
  /** 备注/原因输入占位 */
  placeholder?: string;
  confirmText?: string;
  /** 是否必填(驳回/复核必填,确认可空) */
  required?: boolean;
  submitting?: boolean;
  onConfirm: (note: string) => void;
  onClose: () => void;
}

/** 通用操作弹层:展示摘要 + 备注输入 + 确认/取消(收银对账 确认/驳回、对账复核、退款审核共用)。 */
export function NoteActionSheet({
  visible,
  title,
  description,
  placeholder = '请输入备注(可选)',
  confirmText = '确认',
  required = false,
  submitting = false,
  onConfirm,
  onClose,
}: NoteActionSheetProps) {
  const [note, setNote] = useState('');

  // 每次打开重置输入
  useEffect(() => {
    if (visible) {
      setNote('');
    }
  }, [visible]);

  const handleConfirm = useCallback(() => {
    if (required && !note.trim()) {
      return;
    }
    onConfirm(note.trim());
  }, [required, note, onConfirm]);

  return (
    <BottomSheet visible={visible} onClose={onClose} title={title}>
      {description ? (
        <ThemedText type="small" themeColor="textSecondary" style={styles.description}>
          {description}
        </ThemedText>
      ) : null}
      <Input
        multiline
        value={note}
        onChangeText={setNote}
        placeholder={placeholder}
        style={styles.input}
      />
      {required && !note.trim() ? (
        <ThemedText type="caption" themeColor="danger" style={styles.hint}>
          该项为必填,请填写后提交
        </ThemedText>
      ) : null}
      <View style={styles.actions}>
        <Button title="取消" variant="ghost" size="sm" style={styles.button} onPress={onClose} />
        <Button
          title={submitting ? '提交中...' : confirmText}
          size="sm"
          style={styles.button}
          loading={submitting}
          onPress={handleConfirm}
        />
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  description: {
    marginBottom: Spacing.three,
  },
  input: {
    marginBottom: Spacing.two,
  },
  hint: {
    marginBottom: Spacing.two,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: Spacing.two,
    marginTop: Spacing.two,
  },
  button: {
    minWidth: 96,
  },
});
