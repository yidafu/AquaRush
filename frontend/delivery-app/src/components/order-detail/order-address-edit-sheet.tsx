import { useCallback, useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, View } from 'react-native';

import { BottomSheet } from '@/components/bottom-sheet';
import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { getCurrentPosition } from '@/utils/location';
import type { Address, UpdateOrderAddressInput } from '@aquarush/common';

const PHONE_RE = /^1[3-9]\d{9}$/;

interface OrderAddressEditSheetProps {
  visible: boolean;
  /** 当前订单关联地址(编辑回显;无地址时隐藏编辑入口) */
  address?: Address | null;
  saving: boolean;
  onClose: () => void;
  onSave: (input: UpdateOrderAddressInput) => void;
}

/**
 * 订单页地址信息修正弹层(F-ORD-024)
 * 送水工在配送任务中修正收货人/电话/详细地址/楼层/定位(WGS-84 经纬度)。
 * 仅填写(非空)字段提交;留空字段不修改,空字符串按「不修改」处理。
 */
export function OrderAddressEditSheet({
  visible,
  address,
  saving,
  onClose,
  onSave,
}: OrderAddressEditSheetProps) {
  const [receiverName, setReceiverName] = useState('');
  const [phone, setPhone] = useState('');
  const [detailAddress, setDetailAddress] = useState('');
  const [floor, setFloor] = useState('');
  const [longitude, setLongitude] = useState('');
  const [latitude, setLatitude] = useState('');
  const [locating, setLocating] = useState(false);

  // 每次打开时以当前地址回显
  useEffect(() => {
    if (!visible) return;
    setReceiverName(address?.receiverName ?? '');
    setPhone(address?.phone ?? '');
    setDetailAddress(address?.detailAddress ?? '');
    setFloor(address?.floor != null ? String(address.floor) : '');
    setLongitude(address?.longitude != null ? String(address.longitude) : '');
    setLatitude(address?.latitude != null ? String(address.latitude) : '');
  }, [visible, address]);

  const handleUseCurrentLocation = useCallback(async () => {
    setLocating(true);
    try {
      const position = await getCurrentPosition();
      if (!position) {
        Alert.alert('定位失败', '无法获取当前位置,请检查定位权限后重试,或手动输入经纬度');
        return;
      }
      setLongitude(position.longitude.toFixed(6));
      setLatitude(position.latitude.toFixed(6));
    } finally {
      setLocating(false);
    }
  }, []);

  const handleSave = useCallback(() => {
    const name = receiverName.trim();
    const phoneValue = phone.trim();
    const addressValue = detailAddress.trim();
    const floorValue = floor.trim();
    const lngValue = longitude.trim();
    const latValue = latitude.trim();

    if (phoneValue && phoneValue !== address?.phone && !PHONE_RE.test(phoneValue)) {
      Alert.alert('手机号格式不正确');
      return;
    }

    let floorNum: number | undefined;
    if (floorValue) {
      floorNum = Number(floorValue);
      if (!Number.isInteger(floorNum) || floorNum < 1) {
        Alert.alert('楼层无效', '楼层必须为大于 0 的整数');
        return;
      }
      if (floorNum === address?.floor) floorNum = undefined;
    }

    const lngNum = lngValue ? Number(lngValue) : null;
    const latNum = latValue ? Number(latValue) : null;
    if (lngNum != null || latNum != null) {
      if (lngNum == null || latNum == null) {
        Alert.alert('定位不完整', '经度与纬度需同时填写');
        return;
      }
      if (Number.isNaN(lngNum) || lngNum < -180 || lngNum > 180 || Number.isNaN(latNum) || latNum < -90 || latNum > 90) {
        Alert.alert('坐标无效', '经度范围 -180~180,纬度范围 -90~90');
        return;
      }
    }

    const input: UpdateOrderAddressInput = {
      ...(name && name !== address?.receiverName ? { receiverName: name } : {}),
      ...(phoneValue && phoneValue !== address?.phone ? { phone: phoneValue } : {}),
      ...(addressValue && addressValue !== address?.detailAddress ? { detailAddress: addressValue } : {}),
      ...(floorNum != null ? { floor: floorNum } : {}),
      ...(lngNum != null && (lngNum !== address?.longitude || latNum !== address?.latitude)
        ? { longitude: lngNum, latitude: latNum }
        : {}),
    };

    if (Object.keys(input).length === 0) {
      Alert.alert('未修改', '请填写需要修改的信息');
      return;
    }
    onSave(input);
  }, [receiverName, phone, detailAddress, floor, longitude, latitude, address, onSave]);

  return (
    <BottomSheet visible={visible} onClose={onClose} title="修改收货信息">
      <View style={styles.field}>
        <ThemedText type="smallBold" style={styles.label}>
          收货人姓名
        </ThemedText>
        <Input value={receiverName} onChangeText={setReceiverName} placeholder="收货人姓名(留空=不修改)" maxLength={20} />
      </View>

      <View style={styles.field}>
        <ThemedText type="smallBold" style={styles.label}>
          手机号
        </ThemedText>
        <Input
          value={phone}
          onChangeText={setPhone}
          placeholder="收货人手机号(留空=不修改)"
          keyboardType="phone-pad"
          maxLength={11}
        />
      </View>

      <View style={styles.field}>
        <ThemedText type="smallBold" style={styles.label}>
          详细地址
        </ThemedText>
        <Input
          value={detailAddress}
          onChangeText={setDetailAddress}
          placeholder="详细地址(留空=不修改)"
          multiline
          maxLength={200}
        />
      </View>

      <View style={styles.field}>
        <ThemedText type="smallBold" style={styles.label}>
          楼层
        </ThemedText>
        <Input
          value={floor}
          onChangeText={setFloor}
          placeholder="如 6(留空=不修改;楼层影响配送费判定)"
          keyboardType="number-pad"
          maxLength={3}
        />
      </View>

      <View style={styles.field}>
        <ThemedText type="smallBold" style={styles.label}>
          定位(经纬度)
        </ThemedText>
        <Button
          title="使用当前位置"
          size="md"
          variant="ghost"
          loading={locating}
          onPress={handleUseCurrentLocation}
          style={styles.locationButton}
        />
        <View style={styles.coordRow}>
          <Input
            value={longitude}
            onChangeText={setLongitude}
            placeholder="经度"
            keyboardType="decimal-pad"
            containerStyle={styles.coordInput}
          />
          <Input
            value={latitude}
            onChangeText={setLatitude}
            placeholder="纬度"
            keyboardType="decimal-pad"
            containerStyle={styles.coordInput}
          />
        </View>
        <ThemedText type="caption" themeColor="textTertiary" style={styles.hint}>
          坐标为 WGS-84(与导航/地图一致);留空=不修改,经纬度需成对填写
        </ThemedText>
      </View>

      <View style={styles.actions}>
        <Button title="保存修改" size="lg" loading={saving} onPress={handleSave} style={styles.actionButton} />
        <Button title="取消" size="lg" variant="ghost" onPress={onClose} style={styles.actionButton} disabled={saving} />
      </View>
      <Pressable onPress={onClose} hitSlop={8}>
        <ThemedText type="small" themeColor="textTertiary" style={styles.cancelText}>
          关闭
        </ThemedText>
      </Pressable>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  field: {
    marginBottom: Spacing.three,
  },
  label: {
    marginBottom: Spacing.one,
  },
  locationButton: {
    marginBottom: Spacing.two,
    alignSelf: 'flex-start',
  },
  coordRow: {
    flexDirection: 'row',
    gap: Spacing.two,
  },
  coordInput: {
    flex: 1,
  },
  hint: {
    marginTop: Spacing.one,
  },
  actions: {
    flexDirection: 'row',
    gap: Spacing.two,
    marginTop: Spacing.two,
  },
  actionButton: {
    flex: 1,
  },
  cancelText: {
    textAlign: 'center',
    marginTop: Spacing.three,
  },
});
