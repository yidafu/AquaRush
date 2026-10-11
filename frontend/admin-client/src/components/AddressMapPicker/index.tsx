import React, { useState, useEffect, useCallback } from 'react';
import { InputNumber, Card, Button, Input, message } from '@/ui';
import { EnvironmentOutlined, AimOutlined, ReloadOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { gcj02ToWgs84, wgs84ToGcj02 } from '@aquarush/common';
import MapLibreMap from '../MapLibreMap';
import { geocodeAddress, reverseGeocodeAddress } from '../../lib/map/geocoding';

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

export interface AddressMapPickerProps {
  province?: string;
  city?: string;
  district?: string;
  detailAddress?: string;
  longitude?: number;
  latitude?: number;
  onChange?: (location: {
    longitude: number;
    latitude: number;
    address?: string;
  }) => void;
  disabled?: boolean;
  autoLocate?: boolean;
  onLocateSuccess?: (location: {
    longitude: number;
    latitude: number;
    address?: string;
  }) => void;
}

// Default location (Beijing)
const DEFAULT_LATITUDE = 39.9042;
const DEFAULT_LONGITUDE = 116.4074;

export const AddressMapPicker: React.FC<AddressMapPickerProps> = ({
  province = '',
  city = '',
  district = '',
  detailAddress = '',
  longitude,
  latitude,
  onChange,
  disabled = false,
  autoLocate = false,
  onLocateSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [mapReady, setMapReady] = useState(false);
  const [inputLng, setInputLng] = useState<number | undefined>(longitude);
  const [inputLat, setInputLat] = useState<number | undefined>(latitude);
  const [fullAddress, setFullAddress] = useState<string>('');
  const [marker, setMarker] = useState<{
    id: string;
    lng: number;
    lat: number;
  } | null>(null);

  // Build full address
  useEffect(() => {
    const parts = [province, city, district, detailAddress].filter(Boolean);
    setFullAddress(parts.join(''));
  }, [province, city, district, detailAddress]);

  // Sync coordinate props to state
  useEffect(() => {
    if (longitude !== undefined) {
      setInputLng(longitude);
    }
    if (latitude !== undefined) {
      setInputLat(latitude);
    }
  }, [longitude, latitude]);

  // 展示坐标:存储/接口坐标统一为 WGS-84(与 MapLibre / 天地图底图一致),直接上图
  const displayLng = longitude ?? inputLng ?? DEFAULT_LONGITUDE;
  const displayLat = latitude ?? inputLat ?? DEFAULT_LATITUDE;

  // Update marker when coordinates change
  useEffect(() => {
    if (mapReady) {
      setMarker({ id: 'marker', lng: displayLng, lat: displayLat });
    }
  }, [mapReady, displayLng, displayLat]);

  // Auto geocode when address changes (if autoLocate is enabled)
  const [prevFullAddress, setPrevFullAddress] = useState<string>('');

  useEffect(() => {
    if (autoLocate && fullAddress && fullAddress !== prevFullAddress && mapReady && !loading) {
      setPrevFullAddress(fullAddress);

      // Skip if we already have valid coordinates
      const hasValidCoords = (longitude ?? inputLng) && (latitude ?? inputLat);
      if (hasValidCoords) {
        return;
      }

      // Trigger geocoding
      const doGeocode = async () => {
        setLoading(true);
        try {
          const result = await geocodeAddress(fullAddress, city || undefined);

          if (result) {
            // 腾讯解析返回 GCJ-02 → 转 WGS-84 入库(后端坐标体系统一)
            const wgs = gcj02ToWgs84(result.longitude, result.latitude);
            setInputLng(wgs.longitude);
            setInputLat(wgs.latitude);

            onChange?.({
              longitude: wgs.longitude,
              latitude: wgs.latitude,
              address: result.address,
            });
            onLocateSuccess?.({
              longitude: wgs.longitude,
              latitude: wgs.latitude,
              address: result.address,
            });
          }
        } catch (error) {
          console.error('Auto geocoding error:', error);
        } finally {
          setLoading(false);
        }
      };

      doGeocode();
    }
  }, [fullAddress, autoLocate, mapReady, loading, longitude, inputLng, latitude, inputLat, city, prevFullAddress, onChange, onLocateSuccess]);

  // Handle map ready
  const handleMapInited = useCallback(() => {
    setMapReady(true);
  }, []);

  // Handle geocode button click
  const handleGeocode = useCallback(async () => {
    if (!fullAddress) {
      message.warning('请先选择省市区并输入详细地址');
      return;
    }

    setLoading(true);
    try {
      const result = await geocodeAddress(fullAddress, city || undefined);

      if (result) {
        // 腾讯解析返回 GCJ-02 → 转 WGS-84 入库(后端坐标体系统一)
        const wgs = gcj02ToWgs84(result.longitude, result.latitude);
        setInputLng(wgs.longitude);
        setInputLat(wgs.latitude);

        onChange?.({
          longitude: wgs.longitude,
          latitude: wgs.latitude,
          address: result.address,
        });
        message.success('地址定位成功');
      } else {
        message.error('地址定位失败，请检查地址是否正确');
      }
    } catch (error) {
      console.error('Geocoding error:', error);
      message.error('地址定位失败，请手动输入坐标');
    } finally {
      setLoading(false);
    }
  }, [fullAddress, city, onChange]);

  // 地图/标记点选:MapLibre 点击即为 WGS-84,直接入库(后端坐标体系统一);
  // 仅腾讯逆地理编码请求需要 GCJ-02,单独转换
  const handleMapClick = useCallback(
    async (lngLat: { lng: number; lat: number }) => {
      setInputLng(lngLat.lng);
      setInputLat(lngLat.lat);

      // Reverse geocode to get address(腾讯 API 入参为 GCJ-02)
      const gcj = wgs84ToGcj02(lngLat.lng, lngLat.lat);
      const address = await reverseGeocodeAddress(gcj.longitude, gcj.latitude);

      onChange?.({
        longitude: lngLat.lng,
        latitude: lngLat.lat,
        address,
      });

      message.success('已选择位置');
    },
    [onChange],
  );

  // Handle coordinate input change
  const handleLngChange = useCallback((value: number | null) => {
    const newLng = value ?? DEFAULT_LONGITUDE;
    setInputLng(newLng);

    const currentLat = latitude ?? inputLat ?? DEFAULT_LATITUDE;
    onChange?.({
      longitude: newLng,
      latitude: currentLat,
    });
  }, [latitude, inputLat, onChange]);

  const handleLatChange = useCallback((value: number | null) => {
    const newLat = value ?? DEFAULT_LATITUDE;
    setInputLat(newLat);

    const currentLng = longitude ?? inputLng ?? DEFAULT_LONGITUDE;
    onChange?.({
      longitude: currentLng,
      latitude: newLat,
    });
  }, [longitude, inputLng, onChange]);

  return (
    <div>
      {/* Coordinates Input */}
      <div {...stylex.props(styles.coordinateRow)}>
        <div {...stylex.props(styles.field)}>
          <label {...stylex.props(styles.label)}>经度</label>
          <InputNumber
            {...stylex.props(styles.coordinateInput)}
            value={displayLng}
            onChange={handleLngChange}
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
            onChange={handleLatChange}
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
              onClick={handleGeocode}
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
            onClick={handleMapClick}
            disabled={disabled}
            onMapLoad={handleMapInited}
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

export default AddressMapPicker;
