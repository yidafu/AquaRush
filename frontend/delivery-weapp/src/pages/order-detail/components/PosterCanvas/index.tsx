import { View } from '@tarojs/components'
import { getCurrentInstance } from '@tarojs/taro'
import { useRef, useImperativeHandle, forwardRef } from 'react'
import './index.scss'
import { buildUrl } from '@aquarush/common'

// 格式化日期
const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 手机号脱敏
const maskPhone = (phone: string): string => {
  if (!phone || phone.length < 7) return phone
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

interface OrderData {
  product?: {
    name?: string
    coverImageUrl?: string
  }
  address?: {
    receiverName?: string
    phone?: string
    province?: string
    city?: string
    district?: string
    detailAddress?: string
  }
  quantity?: number
  amount?: number
  orderNo?: string
  createdAt?: string
}

export interface PosterCanvasRef {
  render: (order: OrderData) => Promise<string>
}

interface PosterCanvasProps {
  onReady?: (ref: PosterCanvasRef) => void
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const PosterCanvas = forwardRef<PosterCanvasRef>((props: PosterCanvasProps, ref) => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const wxmlToCanvasRef = useRef<any>(null)

  // 构建海报数据
  const buildWxmlAndStyle = (order: OrderData) => {
    const { product, address, quantity, amount, orderNo, createdAt } = order || {}
    const fullAddress = address
      ? `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
      : ''
    const maskedPhone = address?.phone ? maskPhone(address.phone) : ''

    const wxml = `
<view class="posterTemplate">
  <view class="posterProductRow">
    <view class="posterImagePlaceholder">
        <image src="${buildUrl(product?.coverImageUrl)}" style="width: 80px; height: 80px"/>
    </view>
    <view class="posterProductInfo">
      <text class="posterProductName">
      ${(product?.name || '商品').slice(0, 12)}
      </text>
      <view class="posterQuantityView">
         <text class="posterProductQuantity">X ${quantity || 1}</text>
      </view>
      <text class="posterProductPrice">¥${((amount || 0) / 100).toFixed(2)}</text>
    </view>
  </view>
  <view class="posterAddressSection">
    <view class="posterSectionTitle">
      <text>📍 派送地址</text>
    </view>
    <view class="posterAddressContent">
      <text class="posterReceiver">${address?.receiverName || ''} ${maskedPhone}</text>
      <text class="posterAddressDetail">${fullAddress}</text>
    </view>
  </view>
  <view class="posterFooter">
    <text class="posterOrderNo">订单号: ${orderNo || ''}</text>
    <text class="posterTime">下单时间: ${createdAt ? formatDate(createdAt) : ''}</text>
  </view>
</view>
`
    const style = {
      posterTemplate: {
        width: 380,
        height: 304,
        backgroundColor: '#ffffff',
        padding: 20,
        borderWidth: 1,
        borderRadius: 20,
        borderColor: '#f5f5f4'
      },

      posterProductRow: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
      },
      posterImagePlaceholder: {
        width: 80,
        height: 80,
        borderRadius: 8,
        marginRight: 15,
        backgroundColor: '#f5f5f5',
      },
      posterProductInfo: {
        flex: 1,
        height: 80,
        padding: 8,
        position: 'relative',
      },
      posterProductName: {
        top: 0,
        fontSize: 16,
        color: '#333333',
        marginBottom: 16,
        display: 'flex',
        justifyContent: 'space-between',
      },
      posterQuantityView: {
        top: -16,
        left: 220,
        height: 14,
        width: 30,
      },
      posterProductQuantity: {
        fontSize: 14,
        color: '#999999',
      },
      posterProductPrice: {
        fontSize: 16,
        color: '#ff4d4f',
        fontWeight: 'bold',
      },
      posterAddressSection: {
        borderTopWidth: 1,
        borderTopColor: '#f0f0f0',
        height: 80,
      },
      posterSectionTitle: {
        top: 0,
      },
      posterAddressContent: {
        top: 24,
        left: 16,
      },
      posterReceiver: {
        top: 0,
        fontSize: 14,
        color: '#666666',
        marginBottom: 8,
      },
      posterAddressDetail: {
        top: 16,
        fontSize: 14,
        color: '#666666',
      },
      posterFooter: {
        top: 0,
        backgroundColor: '#f8f8f8',
        padding: 15,
        borderRadius: 8,
        height: 60,
      },
      posterOrderNo: {
        fontSize: 12,
        color: '#666666',
        marginBottom: 8,
      },
      posterTime: {
        top: 12,
        fontSize: 12,
        color: '#666666',
      },
    }
    return { wxml, style }
  }

  // 渲染海报
  const renderPoster = async (order: OrderData): Promise<string> => {
    if (!wxmlToCanvasRef.current) {
      const { page } = getCurrentInstance()
      wxmlToCanvasRef.current = page.selectComponent('#poster-canvas')
    }

    const { wxml, style } = buildWxmlAndStyle(order)
    console.log('order', order)
    console.log('wxml', wxml)
    console.log('style', style)
    try {
      await wxmlToCanvasRef.current.renderToCanvas({ wxml, style })
      const result = await wxmlToCanvasRef.current.canvasToTempFilePath()
      return result.tempFilePath
    } catch (error) {
      console.error('生成海报失败:', error)
      throw error
    }
  }

  useImperativeHandle(ref, () => ({
    render: renderPoster,
  }))


  return (
    <View style={{ position: 'fixed', zIndex: -9999, top: '1000px', right: '1000px' }}>
      {/* @ts-ignore */}
      <wxml-to-canvas
        id='poster-canvas'
        class='wxml-to-canvas'
      />
    </View>
  )
})

export default PosterCanvas
