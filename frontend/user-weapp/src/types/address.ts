// Address types for frontend data structures
// GraphQL types from @aquarush/common are imported directly where needed

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
}

