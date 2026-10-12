import React from 'react';
import { InputNumber, Card, Button, Input } from '@/ui';
import { EnvironmentOutlined, AimOutlined, ReloadOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import MapLibreMap from '../MapLibreMap';

// 局部色值 #666 / #999 / rgba(0,0,0,0.6) 为原有文字与浮层底色,共享 token 无对应值,保持原样
const styles = stylex.create({
  coordinateRow: {
    display: 'flex',
    columnGap: '16px',
    rowGap: '16px',
    marginBottom: '16px',
  },
  field: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
  },
  label: {
    display: 'block',
    marginBottom: '4px',
    fontSize: '12px',
    color: '#666',
  },
  coordinateInput: {
    width: '100%',
  },
  addressRow: {
    display: 'flex',
    columnGap: '8px',
    rowGap: '8px',
    marginBottom: '16px',
  },
  addressInput: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
  },
  mapCard: {
    marginBottom: '16px',
    overflow: 'hidden',
  },
  mapWrap: {
    position: 'relative',
    height: '350px',
  },
  coordBadge: {
    position: 'absolute',
    bottom: '8px',
    left: '8px',
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    color: tokens.onBrand,
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    paddingRight: '8px',
    borderRadius: '4px',
    fontSize: '12px',
    zIndex: 1000,
    pointerEvents: 'none',
  },
  help: {
    fontSize: '12px',
    color: '#999',
  },
});

export interface AddressMapPickerViewProps {
  displayLng: number;
  displayLat: number;
  disabled: boolean;
  fullAddress: string;
  loading: boolean;
  marker: { id: string; lng: number; lat: number } | null;
  onLngChange: (value: number | null) => void;
  onLatChange: (value: number | null) => void;
  onGeocode: () => void;
  onMapClick: (lngLat: { lng: number; lat: number }) => void;
  onMapLoad: () => void;
}

/** 纯展示视图:坐标输入、地址行、交互地图与提示文案(AddressMapPicker 的渲染部分) */
const AddressMapPickerView: React.FC<AddressMapPickerViewProps> = ({
  displayLng,
  displayLat,
  disabled,
  fullAddress,
  loading,
  marker,
  onLngChange,
  onLatChange,
  onGeocode,
  onMapClick,
  onMapLoad,
}) => {
  return (
    <div>
      {/* Coordinates Input */}
      <div {...stylex.props(styles.coordinateRow)}>
        <div {...stylex.props(styles.field)}>
          <label {...stylex.props(styles.label)}>经度</label>
          <InputNumber
            {...stylex.props(styles.coordinateInput)}
            value={displayLng}
            onChange={onLngChange}
            step={0.0001}
            precision={6}
            disabled={disabled}
            placeholder="请输入经度"
          />
        </div>
        <div {...stylex.props(styles.field)}>
          <label {...stylex.props(styles.label)}>纬度</label>
          <InputNumber
            {...stylex.props(styles.coordinateInput)}
            value={displayLat}
            onChange={onLatChange}
            step={0.0001}
            precision={6}
            disabled={disabled}
            placeholder="请输入纬度"
          />
        </div>
      </div>

      {/* Address Display and Geocode Button */}
      <div {...stylex.props(styles.addressRow)}>
        <Input
          {...stylex.props(styles.addressInput)}
          value={fullAddress}
          readOnly
          placeholder="完整地址"
          prefix={<EnvironmentOutlined />}
          suffix={
            <Button
              type="link"
              size="small"
              icon={<ReloadOutlined />}
              onClick={onGeocode}
              disabled={disabled || !fullAddress}
              loading={loading}
            >
              定位
            </Button>
          }
        />
      </div>

      {/* Interactive Map Display */}
      <Card
        size="small"
        styles={{ body: { padding: 0 } }}
        {...stylex.props(styles.mapCard)}
      >
        <div {...stylex.props(styles.mapWrap)}>
          <MapLibreMap
            center={{ lng: displayLng, lat: displayLat }}
            zoom={16}
            height={350}
            markers={marker ? [marker] : []}
            onClick={onMapClick}
            disabled={disabled}
            onMapLoad={onMapLoad}
          />
          <div {...stylex.props(styles.coordBadge)}>
            <AimOutlined /> {displayLng.toFixed(4)}, {displayLat.toFixed(4)}
          </div>
        </div>
      </Card>

      {/* Help Text */}
      <div {...stylex.props(styles.help)}>
        提示：点击"定位"按钮根据地址自动获取坐标，或手动输入经纬度坐标。也可以直接点击地图或标记选择位置（底图:天地图）
      </div>
    </div>
  );
};

export default AddressMapPickerView;
