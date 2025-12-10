// Address types for GraphQL operations and frontend data structures

// GraphQL input types based on backend schema
export interface AddressInput {
  receiverName: string
  phone: string
  province: string
  provinceCode?: string
  city: string
  cityCode?: string
  district: string
  districtCode?: string
  detailAddress: string
  longitude?: number
  latitude?: number
  isDefault: boolean
}

// GraphQL response types
export interface Address {
  id: number
  userId: number
  receiverName: string
  phone: string
  province: string
  provinceCode?: string
  city: string
  cityCode?: string
  district: string
  districtCode?: string
  detailAddress: string
  longitude?: number
  latitude?: number
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

// GraphQL mutation and query response types
export interface CreateAddressResponse {
  createAddress: Address
}

export interface UpdateAddressResponse {
  updateAddress: Address
}

export interface DeleteAddressResponse {
  deleteAddress: boolean
}

export interface SetDefaultAddressResponse {
  setDefaultAddress: boolean
}

export interface UserAddressesResponse {
  userAddresses: Address[]
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
}

// Helper function to transform form data to GraphQL input
export function transformFormToGraphQLInput(formData: AddressFormData): AddressInput {
  return {
    receiverName: formData.receiverName,
    phone: formData.phone,
    province: formData.province,
    city: formData.city,
    district: formData.district,
    detailAddress: formData.detailAddress,
    longitude: formData.longitude || undefined,
    latitude: formData.latitude || undefined,
    isDefault: formData.isDefault,
    provinceCode: formData.provinceCode || undefined,
    cityCode: formData.cityCode || undefined,
    districtCode: formData.districtCode || undefined
  }
}