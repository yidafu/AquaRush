// Simplified GraphQL types for User WeApp
export interface Product {
  id: string
  name: string
  subtitle?: string
  price: string
  originalPrice?: string
  depositPrice?: string
  coverImageUrl: string
  imageGallery?: any[]
  specification: string
  waterSource?: string
  phValue?: string
  mineralContent?: string
  stock: number
  salesVolume: number
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE'
  sortOrder: number
  tags?: any[]
  detailContent?: string
  certificateImages?: any[]
  deliverySettings?: Record<string, any>
  isDeleted: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductResponse {
  product?: Product
}

export interface GraphQLResponse<T = any> {
  data?: T
  errors?: Array<{
    message: string
    locations?: Array<{
      line: number
      column: number
    }>
    path?: Array<string | number>
    extensions?: Record<string, any>
  }>
}