import { useMemo } from 'react';
import { centsToYuan, yuanToCents, displayCents } from '@aquarush/common';

export interface Product {
  id: number;
  name: string;
  subtitle?: string;
  priceCents: number; // 销售价（分）
  originalPriceCents?: number; // 原价（分）
  depositPriceCents?: number; // 押金（分）
  coverImageUrl: string;
  imageGallery?: string[] | string; // JSON array from backend
  specification: string;
  waterSource?: string;
  mineralContent?: string;
  stock: number;
  salesVolume: number;
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE';
  sortOrder: number;
  tags?: string[] | string; // JSON array from backend
  detailContent?: string;
  certificateImages?: string[] | string; // JSON array from backend
  createdAt: string;
  updatedAt: string;
}

export interface ProductFormData {
  name: string;
  subtitle?: string;
  price: number; // in yuan (元) for form display
  originalPrice?: number; // in yuan (元) for form display
  depositPrice?: number; // in yuan (元) for form display
  coverImageUrl?: string;
  imageGallery?: string[]; // Form handles arrays directly
  specification: string;
  waterSource?: string;
  mineralContent?: string;
  stock: number;
  salesVolume: number;
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK';
  sortOrder: number;
  tags?: string[]; // Form handles arrays directly
  detailContent?: string;
  certificateImages?: string[]; // Form handles arrays directly
}

/**
 * 将数据库中的商品数据转换为表单数据格式
 * 主要处理价格从分转换为元
 */
export function productToFormData(product: Product): ProductFormData {
  // Parse JSON fields if they are strings
  const parseArrayField = (field?: string | string[]): string[] => {
    if (!field) return [];
    if (Array.isArray(field)) return field;
    if (typeof field === 'string') {
      try {
        const parsed = JSON.parse(field);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    }
    return [];
  };

  return {
    ...product,
    price: centsToYuan(product.priceCents),
    originalPrice: product.originalPriceCents ? centsToYuan(product.originalPriceCents) : undefined,
    depositPrice: product.depositPriceCents ? centsToYuan(product.depositPriceCents) : undefined,
    status: product.status === 'ACTIVE' ? 'ONLINE' : product.status,
    // Convert JSON string fields to arrays for form
    imageGallery: parseArrayField(product.imageGallery),
    tags: parseArrayField(product.tags),
    certificateImages: parseArrayField(product.certificateImages),
  };
}

/**
 * 将表单数据转换为数据库格式
 * 主要处理价格从元转换为分
 */
export function formDataToProduct({ price, originalPrice, depositPrice, ...formData }: ProductFormData): Partial<Product> {
  const result: Partial<Product> = {
    ...formData,
    priceCents: yuanToCents(price),
    originalPriceCents: originalPrice ? yuanToCents(originalPrice) : undefined,
    depositPriceCents: depositPrice ? yuanToCents(depositPrice) : undefined,
    salesVolume: formData.salesVolume || 0,
    sortOrder: formData.sortOrder || 0,
  };

  // Handle array fields - send as arrays directly for GraphQL
  // The ArrayNodeConverter on the backend will handle the conversion to JSON
  if (Array.isArray(formData.imageGallery)) {
    result.imageGallery = formData.imageGallery;
  }

  if (Array.isArray(formData.certificateImages)) {
    result.certificateImages = formData.certificateImages;
  }

  if (Array.isArray(formData.tags)) {
    result.tags = formData.tags;
  }

  return result;
}

/**
 * Hook: 格式化价格显示
 */
export function useFormattedPrices(product: Product) {
  return useMemo(() => ({
    price: displayCents(product.priceCents),
    originalPrice: product.originalPriceCents ? displayCents(product.originalPriceCents) : null,
    depositPrice: product.depositPriceCents ? displayCents(product.depositPriceCents) : null,
  }), [product.priceCents, product.originalPriceCents, product.depositPriceCents]);
}

/**
 * Hook: 解析 JSON 字段
 */
export function useParsedJSONFields(product: Product) {
  return useMemo(() => {
    const parseJSONField = (field?: string | string[]) => {
      if (!field) return [];
      if (Array.isArray(field)) return field;
      if (typeof field === 'string') {
        try {
          const parsed = JSON.parse(field);
          return Array.isArray(parsed) ? parsed : [];
        } catch {
          return [];
        }
      }
      return [];
    };

    return {
      imageGallery: parseJSONField(product.imageGallery),
      certificateImages: parseJSONField(product.certificateImages),
      tags: parseJSONField(product.tags),
    };
  }, [product.imageGallery, product.certificateImages, product.tags]);
}

/**
 * Hook: 格式化商品状态显示
 */
export function useFormattedStatus(product: Product) {
  return useMemo(() => {
    const statusMap = {
      'ONLINE': { text: '✓ 在线', color: '#52c41a' },
      'OFFLINE': { text: '✗ 下线', color: '#ff4d4f' },
      'OUT_OF_STOCK': { text: '△ 缺货', color: '#fa8c16' },
      'ACTIVE': { text: '✓ 在线', color: '#52c41a' },
    };

    return statusMap[product.status] || statusMap['OFFLINE'];
  }, [product.status]);
}

/**
 * Hook: 库存状态提示
 */
export function useStockStatus(product: Product) {
  return useMemo(() => {
    const isLowStock = product.stock < 10;
    return {
      stock: product.stock,
      isLowStock,
      stockText: `${product.stock} 件${isLowStock ? ' (低库存)' : ''}`,
      stockColor: isLowStock ? '#ff4d4f' : 'inherit',
      stockWeight: isLowStock ? 'bold' : 'normal',
    };
  }, [product.stock]);
}
