import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { View, Map, Text, BaseEventOrig, MapProps } from '@tarojs/components'
import { AtIcon, AtToast } from 'taro-ui'
import { geocoder, IGeocoderOptions, IGeocoderResult } from '@/libs/qqmap'


const debounce = (callback: Function, delay: number) => {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return (...args: any[]) => {
    if (timeoutId !== null) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      callback(...args)
    }, delay)
  }
}
interface AddressMapProps {
  address?: string;         // 详细地址
  province?: string;        // 省份
  city?: string;           // 城市
  district?: string;       // 区域
  latitude?: number;       // 纬度坐标，优先级高于地址解析
  longitude?: number;      // 经度坐标，优先级高于地址解析
  className?: string;      // 自定义样式类名
  height?: string;         // 地图高度，默认400px
  showControls?: boolean;  // 是否显示地图控件
  onLocationChange?: (location: { latitude: number; longitude: number; address: string }) => void;
}

interface MapLocationState {
  latitude: number;
  longitude: number;
  address: string;
  loading: boolean;
  error: string;
  showToastVisible: boolean;
  toastText: string;
  toastType: 'success' | 'error' | 'loading';
}
// 默认坐标（北京市中心）
const defaultLocation = {
  latitude: 39.9042,
  longitude: 116.4074,
  address: '北京市'
}

const AddressMap: React.FC<AddressMapProps> = ({
  address,
  province = '',
  city = '',
  district = '',
  latitude,
  longitude,
  className = '',
  height = '400px',
  showControls = true,
  onLocationChange
}) => {

  const [location, setLocation] = useState<MapLocationState>({
    latitude: defaultLocation.latitude,
    longitude: defaultLocation.longitude,
    address: defaultLocation.address,
    loading: false,
    error: '',
    showToastVisible: false,
    toastText: '',
    toastType: 'success'
  })

  const [markers, setMarkers] = useState<MapProps.marker[]>([])

  // 显示提示信息
  const showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setLocation(prev => ({
      ...prev,
      showToastVisible: true,
      toastText: text,
      toastType: type
    }))

    setTimeout(() => {
      setLocation(prev => ({ ...prev, showToastVisible: false }))
    }, 3000)
  }

  // 更新地图标记
  const updateMarker = useCallback((latitude: number, longitude: number, address: string) => {
    if (!address) {
      setMarkers([])
      return
    }
    console.log('update marker', latitude, longitude, address)
    const newMarker: MapProps.marker= {
      id: 1,
      latitude,
      longitude,
      title: address,
      iconPath: '/assets/marker.png',
      width: 30,
      height: 30,
      callout: {
        content: address,
        color: '#333333',
        fontSize: 14,
        borderRadius: 5,
        bgColor: '#ffffff',
        padding: 8,
        display: 'ALWAYS',
        anchorX: 0.5,
        anchorY: 0,
        borderWidth: 0,
        borderColor: '',
        textAlign: 'center'
      }
    }

    setMarkers([newMarker])
  }, [])

  // 地理编码：将地址转换为坐标
  const performGeocoder = useCallback(async (result: IGeocoderResult) => {

    setLocation(prev => ({ ...prev, loading: true, error: '' }))

    try {

      // 杭州市古翠路地铁站
      setLocation({
        latitude: result.latitude,
        longitude: result.longitude,
        address: result.address,
        loading: false,
        error: '',
        showToastVisible: false,
        toastText: '',
        toastType: 'success'
      })

      updateMarker(result.latitude, result.longitude, result.address)
      // showToast('地址定位成功', 'success')

      // 回调父组件
      if (onLocationChange) {
        onLocationChange({
          latitude: result.latitude,
          longitude: result.longitude,
          address: result.address
        })
      }
    } catch (error) {
      console.error('Geocoder error:', error)
      const errorMessage = error instanceof Error ? error.message : '地址解析失败'
      setLocation(prev => ({
        ...prev,
        loading: false,
        error: errorMessage
      }))
      showToast(errorMessage, 'error')
    }
  }, [onLocationChange, updateMarker, showToast])


  const memoDebounceGeocoder = useMemo(() => debounce((options: IGeocoderOptions, callback: (result: IGeocoderResult) => void) => {
    const addressValue = options.address
    if (!addressValue || !addressValue.trim()) {
      // setLocation(prev => ({ ...prev, error: '地址不能为空' }))
      return
    }

    geocoder(options).then(callback);
  }, 1000), [])

  // 使用 ref 记录上一次的地址，避免重复的地理编码请求
  const prevAddressRef = useRef<string>('')

  // 监听地址和坐标变化，自动进行地理编码或直接设置坐标
  useEffect(() => {
    const currentAddress = address?.trim() || ''

    // 如果地址没有变化，不执行任何操作
    if (currentAddress === prevAddressRef.current) {
      return
    }

    // 更新 ref
    prevAddressRef.current = currentAddress

    // 如果提供了坐标，优先使用坐标
    if (currentAddress) {
      // 如果没有坐标但有地址，进行地址解析
      memoDebounceGeocoder({
        address,
        province,
        city,
        district,
      }, performGeocoder)
    } else {
      // 如果既没有坐标也没有地址，显示默认位置
      setLocation(prev => ({
        ...prev,
        latitude: defaultLocation.latitude,
        longitude: defaultLocation.longitude,
        address: defaultLocation.address,
        error: ''
      }))
      updateMarker(defaultLocation.latitude, defaultLocation.longitude, defaultLocation.address)
    }
  }, [address, onLocationChange, updateMarker])

  // 处理地图点击事件
  const handleMapTap = useCallback((event: any) => {
    const { latitude, longitude } = event.detail

    setLocation(prev => ({
      ...prev,
      latitude,
      longitude,
      address: '手动选择位置'
    }))

    updateMarker(latitude, longitude, '手动选择位置')
    // showToast('已选择新位置', 'success')

    // 回调父组件
    if (onLocationChange) {
      onLocationChange({
        latitude,
        longitude,
        address: '手动选择位置'
      })
    }
  }, [onLocationChange, updateMarker, showToast])

  return (
    <View className={`address-map ${className}`}>
      {/* 地图容器 */}
      <View
        className='address-map-container'
        style={{ height }}
      >
        <Map
          className='address-map-component'
          latitude={location.latitude ?? defaultLocation.latitude}
          longitude={location.longitude ?? defaultLocation.longitude}
          scale={16}
          markers={markers}
          showLocation={showControls}
          showScale={showControls}
          showCompass={showControls}
          enableScroll={true}
          enableZoom={true}
          enableRotate={false}
          enable3D={false}
          onTap={handleMapTap}
          style={{
            height, width: '100%'
          }} onError={function (event: BaseEventOrig<MapProps.point>): void {
            console.log('map error', event)
            // throw new Error('Function not implemented.')
          }} />

        {/* 加载状态覆盖层 */}
        {location.loading && (
          <View className='address-map-loading'>
            <Text className='loading-text'>正在定位...</Text>
          </View>
        )}

        {/* 错误状态 */}
        {location.error && (
          <View className='address-map-error'>
            <AtIcon value='close-circle' size='20' color='var(--error-color)' />
            <Text className='error-text'>{location.error}</Text>
          </View>
        )}

      </View>



      {/* Toast 提示 */}
      <AtToast
        isOpened={location.showToastVisible}
        text={location.toastText}
        status={location.toastType}
        duration={3000}
        onClose={() => setLocation(prev => ({ ...prev, showToastVisible: false }))}
      />
    </View>
  )
}

export default AddressMap
