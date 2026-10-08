// Address types for frontend data structures
// GraphQL types from @aquarush/common are imported directly where needed

// Address entity type
export interface Address {
  // 主键为 Snowflake Long,PrimaryId 标量序列化为字符串,禁止转 Number(精度丢失)
  id: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  latitude?: number
  longitude?: number
  // 楼层(空=无楼层信息/平房,配送费楼层判定用 —— R-MON-02)
  floor?: number
}

// Error types for service operations
export interface AddressServiceError {
  message: string
  code?: string
  field?: string
}

// Service method return types
export interface AddressServiceResult<T> {
  success: boolean
  data?: T
  error?: AddressServiceError
}

// Form data types (matches frontend form)
export interface AddressFormData {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  latitude?: number
  longitude?: number
  // 楼层(空=无楼层信息/平房,配送费楼层判定用 —— R-MON-02)
  floor?: number
}

