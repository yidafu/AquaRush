import React, { useState, useEffect, useRef, useCallback } from 'react'
import { View, Text, Textarea, ScrollView } from '@tarojs/components'
import { AtFloatLayout, AtIcon } from 'taro-ui'
import 'taro-ui/dist/style/components/float-layout.scss'
import 'taro-ui/dist/style/components/icon.scss'
import { searchAllAddresses } from '../../services/delivery'
import './index.scss'

export interface Address {
  id: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
}

// 地址列表项组件
interface AddressListItemProps {
  address: Address
  selected?: boolean
  onClick?: (address: Address) => void
}

export const AddressListItem: React.FC<AddressListItemProps> = ({
  address,
  selected = false,
  onClick,
}) => {
  const handleClick = () => {
    onClick?.(address)
  }

  return (
    <View
      className={`address-item ${selected ? 'selected' : ''}`}
      onClick={handleClick}
    >
      <View className='address-content'>
        <View className='address-header'>
          <Text className='receiver-name'>{address.receiverName}</Text>
          <Text className='phone'>{address.phone}</Text>
          {address.isDefault && <Text className='default-tag'>默认</Text>}
        </View>
        <Text className='address-detail'>
          {address.province}{address.city}{address.district}{address.detailAddress}
        </Text>
      </View>
      {selected && (
        <View className='check-icon'>
          <AtIcon value='check' size='20' color='#1890ff' />
        </View>
      )}
    </View>
  )
}

interface AddressDisplayProps {
  selectedId?: string
  loading?: boolean
  onSelect?: (address: Address) => void
}

const AddressDisplay: React.FC<AddressDisplayProps> = ({
  selectedId,
  loading = false,
  onSelect
}) => {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [filteredAddresses, setFilteredAddresses] = useState<Address[]>([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Load all addresses on mount
  useEffect(() => {
    searchAllAddresses('', 20)
      .then((res) => {
        if (res?.searchAllAddresses) {
          setAddresses(res.searchAllAddresses)
          setFilteredAddresses(res.searchAllAddresses)
        }
      })
      .catch((err) => {
        console.error('Load addresses failed:', err)
      })
      .finally(() => {
        setInitialLoading(false)
      })
  }, [])

  // Get the selected address object
  const selectedAddress = addresses.find(addr => addr.id === selectedId)

  // Debounced search function
  const performSearch = useCallback((keyword: string) => {
    if (!keyword.trim()) {
      setFilteredAddresses(addresses)
      setSearchLoading(false)
      return
    }

    setSearchLoading(true)
    searchAllAddresses(keyword, 20)
      .then((res) => {
        if (res?.data?.searchAllAddresses) {
          setFilteredAddresses(res.data.searchAllAddresses)
        }
      })
      .catch((err) => {
        console.error('Search addresses failed:', err)
        // Fallback to local filter on error
        const lowerKeyword = keyword.toLowerCase()
        const filtered = addresses.filter(addr => {
          return (
            addr.phone.includes(lowerKeyword) ||
            addr.receiverName.toLowerCase().includes(lowerKeyword) ||
            addr.province.toLowerCase().includes(lowerKeyword) ||
            addr.city.toLowerCase().includes(lowerKeyword) ||
            addr.district.toLowerCase().includes(lowerKeyword) ||
            addr.detailAddress.toLowerCase().includes(lowerKeyword)
          )
        })
        setFilteredAddresses(filtered)
      })
      .finally(() => {
        setSearchLoading(false)
      })
  }, [addresses])

  // Debounced search when keyword changes
  useEffect(() => {
    if (searchTimerRef.current) {
      clearTimeout(searchTimerRef.current)
    }

    searchTimerRef.current = setTimeout(() => {
      performSearch(searchKeyword)
    }, 300)

    return () => {
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }
    }
  }, [searchKeyword, performSearch])

  // Reset filtered addresses when addresses list changes (without search keyword)
  useEffect(() => {
    if (!searchKeyword.trim()) {
      setFilteredAddresses(addresses)
    }
  }, [addresses, searchKeyword])

  const handleClick = () => {
    if (addresses.length > 0) {
      setModalVisible(true)
    }
  }

  const handleSelect = (address: Address) => {
    onSelect?.(address)
    setModalVisible(false)
    setSearchKeyword('')
  }

  const handleClose = () => {
    setModalVisible(false)
    setSearchKeyword('')
  }

  return (
    <>
      <View
        className='card address-card'
        onClick={handleClick}
      >
        {(loading || initialLoading) ? (
          <Text className='loading-text'>加载中...</Text>
        ) : selectedAddress ? (
          <View className='selected-address'>
            <View className='address-info'>
              <Text className='receiver-name'>{selectedAddress.receiverName}</Text>
              <Text className='phone'>{selectedAddress.phone}</Text>
              {selectedAddress.isDefault && <Text className='default-tag'>默认</Text>}
            </View>
            <Text className='address-detail'>
              {selectedAddress.province}{selectedAddress.city}{selectedAddress.district}{selectedAddress.detailAddress}
            </Text>
            <View className='chevron'>
              <Text className='chevron-text'>›</Text>
            </View>
          </View>
        ) : (
          <Text className='empty-text'>点击选择地址</Text>
        )}
      </View>

      <AtFloatLayout
        isOpened={modalVisible}
        title='选择地址'
        onClose={handleClose}
      >
        {/* Search Bar */}
          <View className='modal-search-bar'>
            <View className='search-input-wrap'>
              <AtIcon value='search' size='16' color='#999' />
              <Textarea
                className='search-input'
                placeholder='搜索手机号、姓名或地址'
                value={searchKeyword}
                onInput={(e) => setSearchKeyword(e.detail.value)}
                adjustPosition
              />
              {searchKeyword && (
                <View className='clear-btn' onClick={() => setSearchKeyword('')}>
                  <AtIcon value='close-circle' size='16' color='#999' />
                </View>
              )}
            </View>
          </View>

          {/* Address List */}
          <ScrollView className='modal-address-list' scrollY>
            {searchLoading ? (
              <View className='empty-state'>
                <Text className='loading-text'>搜索中...</Text>
              </View>
            ) : filteredAddresses.length === 0 ? (
              <View className='empty-state'>
                <Text className='empty-text'>
                  {addresses.length === 0 ? '暂无地址' : '没有匹配的地址'}
                </Text>
              </View>
            ) : (
              filteredAddresses.map(addr => (
                <AddressListItem
                  key={addr.id}
                  address={addr}
                  selected={selectedId === addr.id}
                  onClick={handleSelect}
                />
              ))
            )}
          </ScrollView>
      </AtFloatLayout>
    </>
  )
}

export default AddressDisplay
