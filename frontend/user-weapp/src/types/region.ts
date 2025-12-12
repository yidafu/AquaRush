export interface Region {
  id: string
  name: string
  code: string
  parentCode?: string
  level: number // 1=Province, 2=City, 3=District
  createdAt?: string
  updatedAt?: string
}

export interface RegionHierarchy {
  province: Region
  city: Region
  district: Region
  provinces: Region[]
  cities: Region[]
  districts: Region[]
}

// For backward compatibility - deprecated, use RegionHierarchy instead
export interface RegionHierarchyLegacy {
  provinces: Region[]
  cities: Region[]
  districts: Region[]
}

export interface RegionSelection {
  province?: Region
  city?: Region
  district?: Region
  provinceIndex?: number
  cityIndex?: number
  districtIndex?: number
}

export interface RegionLoadingState {
  provinces: boolean
  cities: boolean
  districts: boolean
}

export interface RegionErrorState {
  provinces?: string
  cities?: string
  districts?: string
  general?: string
}

export interface RegionCacheItem {
  data: Region[]
  timestamp: number
  ttl: number // Time to live in milliseconds
}

export interface RegionsResponse {
  regions: Region[]
}

export interface RegionVariables {
  level?: number
  parentCode?: string
}

export interface DefaultRegionResponse {
  defaultRegionHierarchy: RegionHierarchy | null
}