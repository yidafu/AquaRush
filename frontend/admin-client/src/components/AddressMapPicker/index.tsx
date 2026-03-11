import React, { useState, useEffect, useCallback } from 'react';
import { InputNumber, Card, Spin, Button, Input, message } from 'antd';
import { EnvironmentOutlined, AimOutlined, ReloadOutlined } from '@ant-design/icons';
import { BaseMap, MultiMarker } from 'tlbs-map-react';

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

// Tencent Map key (shared with user-weapp)
const TENCENT_MAP_KEY = '3YEBZ-G233L-GN7P7-M7EN6-YNLOH-VVBTT';

// Marker styles
const markerStyles = {
  marker: {
    width: 20,
    height: 30,
    anchor: { x: 10, y: 30 },
    src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerDefault.png',
  },
};

// Geocoding API
const geocodeAddress = async (address: string, region?: string): Promise<{
  longitude: number;
  latitude: number;
  address: string;
} | null> => {
  if (!address) return null;

  try {
    const encodedAddress = encodeURIComponent(address);
    const url = `https://apis.map.qq.com/ws/geocoder/v1/?address=${encodedAddress}&key=${TENCENT_MAP_KEY}${region ? `&region=${encodeURIComponent(region)}` : ''}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status === 0 && data.result) {
      const location = data.result.location;
      return {
        longitude: location.lng,
        latitude: location.lat,
        address: data.result.title || address,
      };
    } else {
      console.error('Geocoding failed:', data.message);
      return null;
    }
  } catch (error) {
    console.error('Geocoding error:', error);
    return null;
  }
};

// Reverse Geocoding: coordinates to address
const reverseGeocodeAddress = async (lng: number, lat: number): Promise<string> => {
  try {
    const url = `https://apis.map.qq.com/ws/geocoder/v1/?location=${lat},${lng}&key=${TENCENT_MAP_KEY}&get_poi_info=0`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.status === 0 && data.result) {
      return data.result.address;
    }
    return '';
  } catch (error) {
    console.error('Reverse geocoding error:', error);
    return '';
  }
};

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
  const [markerGeometries, setMarkerGeometries] = useState<Array<{
    id: string;
    styleId: string;
    position: { lat: number; lng: number };
  }>>([]);

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

  // Update marker when coordinates change
  useEffect(() => {
    const currentLng = longitude ?? inputLng ?? DEFAULT_LONGITUDE;
    const currentLat = latitude ?? inputLat ?? DEFAULT_LATITUDE;

    if (mapReady) {
      setMarkerGeometries([
        {
          id: 'marker',
          styleId: 'marker',
          position: { lat: currentLat, lng: currentLng },
        },
      ]);
    }
  }, [longitude, latitude, inputLng, inputLat, mapReady]);

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
            setInputLng(result.longitude);
            setInputLat(result.latitude);
            setMarkerGeometries([
              {
                id: 'marker',
                styleId: 'marker',
                position: { lat: result.latitude, lng: result.longitude },
              },
            ]);

            onChange?.({
              longitude: result.longitude,
              latitude: result.latitude,
              address: result.address,
            });
            onLocateSuccess?.({
              longitude: result.longitude,
              latitude: result.latitude,
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
        setInputLng(result.longitude);
        setInputLat(result.latitude);
        setMarkerGeometries([
          {
            id: 'marker',
            styleId: 'marker',
            position: { lat: result.latitude, lng: result.longitude },
          },
        ]);

        onChange?.({
          longitude: result.longitude,
          latitude: result.latitude,
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

  // Handle marker click
  const handleMarkerClick = useCallback(async (e: any) => {
    const { lat, lng } = e.latLng || e.position;
    if (lat && lng) {
      setInputLng(lng);
      setInputLat(lat);

      // Reverse geocode to get address
      const address = await reverseGeocodeAddress(lng, lat);

      onChange?.({
        longitude: lng,
        latitude: lat,
        address,
      });

      message.success('已选择位置');
    }
  }, [onChange]);

  // Handle coordinate input change
  const handleLngChange = useCallback((value: number | null) => {
    const newLng = value ?? DEFAULT_LONGITUDE;
    setInputLng(newLng);

    const currentLat = latitude ?? inputLat ?? DEFAULT_LATITUDE;

    setMarkerGeometries([
      {
        id: 'marker',
        styleId: 'marker',
        position: { lat: currentLat, lng: newLng },
      },
    ]);

    onChange?.({
      longitude: newLng,
      latitude: currentLat,
    });
  }, [latitude, inputLat, onChange]);

  const handleLatChange = useCallback((value: number | null) => {
    const newLat = value ?? DEFAULT_LATITUDE;
    setInputLat(newLat);

    const currentLng = longitude ?? inputLng ?? DEFAULT_LONGITUDE;

    setMarkerGeometries([
      {
        id: 'marker',
        styleId: 'marker',
        position: { lat: newLat, lng: currentLng },
      },
    ]);

    onChange?.({
      longitude: currentLng,
      latitude: newLat,
    });
  }, [longitude, inputLng, onChange]);

  // Display values
  const displayLng = longitude ?? inputLng ?? DEFAULT_LONGITUDE;
  const displayLat = latitude ?? inputLat ?? DEFAULT_LATITUDE;

  return (
    <div className="address-map-picker">
      {/* Coordinates Input */}
      <div className="coordinate-inputs" style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 12, color: '#666' }}>经度</label>
          <InputNumber
            style={{ width: '100%' }}
            value={displayLng}
            onChange={handleLngChange}
            step={0.0001}
            precision={6}
            disabled={disabled}
            placeholder="请输入经度"
          />
        </div>
        <div style={{ flex: 1 }}>
          <label style={{ display: 'block', marginBottom: 4, fontSize: 12, color: '#666' }}>纬度</label>
          <InputNumber
            style={{ width: '100%' }}
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
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <Input
          style={{ flex: 1 }}
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
        style={{ marginBottom: 16, overflow: 'hidden' }}
      >
        <div style={{ position: 'relative', height: 350 }}>
          <BaseMap
            apiKey={TENCENT_MAP_KEY}
            options={{
              zoom: 16,
              center: { lat: displayLat, lng: displayLng },
            }}
            onMapInited={handleMapInited}
          >
            <MultiMarker
              styles={markerStyles}
              geometries={markerGeometries}
              onClick={handleMarkerClick}
            />
          </BaseMap>
          <div
            style={{
              position: 'absolute',
              bottom: 8,
              left: 8,
              background: 'rgba(0,0,0,0.6)',
              color: '#fff',
              padding: '4px 8px',
              borderRadius: 4,
              fontSize: 12,
              zIndex: 1000,
              pointerEvents: 'none',
            }}
          >
            <AimOutlined /> {displayLng.toFixed(4)}, {displayLat.toFixed(4)}
          </div>
        </div>
      </Card>

      {/* Help Text */}
      <div style={{ fontSize: 12, color: '#999' }}>
        提示：点击"定位"按钮根据地址自动获取坐标，或手动输入经纬度坐标。也可以直接点击地图或标记选择位置
      </div>
    </div>
  );
};

export default AddressMapPicker;
