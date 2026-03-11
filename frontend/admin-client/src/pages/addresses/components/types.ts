// Address management types for admin frontend

export interface AddressInput {
  receiverName: string;
  phone: string;
  province: string;
  provinceCode?: string;
  city: string;
  cityCode?: string;
  district: string;
  districtCode?: string;
  detailAddress: string;
  longitude?: number;
  latitude?: number;
  isDefault?: boolean;
}

export interface Address {
  id: string;
  userId: string | null;
  receiverName: string;
  phone: string;
  province: string;
  provinceCode?: string;
  city: string;
  cityCode?: string;
  district: string;
  districtCode?: string;
  detailAddress: string;
  longitude?: number;
  latitude?: number;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddressFormData {
  receiverName: string;
  phone: string;
  province: string;
  provinceCode?: string;
  city: string;
  cityCode?: string;
  district: string;
  districtCode?: string;
  detailAddress: string;
  longitude?: number;
  latitude?: number;
  isDefault: boolean;
}

export interface BatchImportResult {
  successCount: number;
  failureCount: number;
  totalCount: number;
}

export interface ExcelRowData {
  收货人姓名: string;
  手机号: string;
  省: string;
  市: string;
  县: string;
  详细地址: string;
}
