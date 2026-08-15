import React, { useState, useEffect } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { Notice } from '@aquarush/common'
import NoticeService from '../../services/NoticeService'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import './index.scss'

// 公告类型文案
const NOTICE_TYPE_LABELS: Record<string, string> = {
  PLATFORM_NOTICE: '平台公告',
  WATER_OUTAGE: '停水通知',
  ACTIVITY_PREVIEW: '活动预告'
}

/**
 * 公告详情页(F-OPS-002 用户端)
 * 展示公告列表(置顶优先/发布时间倒序) + 选中公告全文;id 来自路由参数,缺省选第 1 条。
 */
const NoticeDetailPage: React.FC = () => {
  usePageViewTracking()

  const router = useRouter()
  const [notices, setNotices] = useState<Notice[]>([])
  const [selectedId, setSelectedId] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(true)

  useEffect(() => {
    loadNotices()
  }, [])

  // 路由参数变化时切换选中公告
  useEffect(() => {
    const id = router.params.id
    if (id) {
      setSelectedId(id)
    }
  }, [router.params.id])

  const loadNotices = async () => {
    try {
      const result = await NoticeService.getActiveNotices()
      if (result.success) {
        setNotices(result.data)
        if (!router.params.id && result.data.length > 0) {
          setSelectedId(result.data[0].id)
        }
      } else {
        throw new Error(result.error.message)
      }
    } catch (error) {
      console.error('Load notices error:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      setLoading(false)
    }
  }

  const selected = notices.find((n) => n.id === selectedId) || null

  return (
    <View className='notice-detail-page'>
      {loading ? (
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      ) : notices.length === 0 ? (
        <View className='empty-container'>
          <Text>暂无公告</Text>
        </View>
      ) : (
        <View className='notice-detail-body'>
          {/* 公告列表(置顶优先/发布时间倒序由服务端排序) */}
          <View className='notice-list'>
            {notices.map((notice) => (
              <View
                key={notice.id}
                className={`notice-list-item ${notice.id === selected?.id ? 'notice-list-item-active' : ''}`}
                onClick={() => setSelectedId(notice.id)}
              >
                {notice.pinned && <Text className='notice-pin-tag'>置顶</Text>}
                <Text className='notice-list-title' numberOfLines={1}>
                  {notice.title}
                </Text>
              </View>
            ))}
          </View>

          {/* 选中公告全文 */}
          {selected && (
            <View className='notice-content-card'>
              <Text className='notice-type-tag'>{NOTICE_TYPE_LABELS[selected.type] || selected.type}</Text>
              <Text className='notice-content-title'>{selected.title}</Text>
              {selected.publishedAt && (
                <Text className='notice-publish-time'>
                  {selected.publishedAt.slice(0, 10)}
                </Text>
              )}
              <View className='notice-content-divider' />
              <Text className='notice-content-text'>{selected.content}</Text>
            </View>
          )}
        </View>
      )}
    </View>
  )
}

export default NoticeDetailPage
