import { yuanToCents } from '@aquarush/common';
import type { ProductFormData } from '../components/hooks/useProductData';

/** 去重并保持先后顺序(配置项在前,当前值兜底补入) */
export function uniq(values: string[]): string[] {
  return [...new Set(values.filter(Boolean))];
}

/**
 * 将表单值转换为 Create/UpdateProductInput 所需结构(products/form/index.tsx 拆分):
 * 价格由元转分;图片数组序列化为 JSON 字符串(与后端 JsonArray 标量兼容)。
 */
export function toProductInput(values: ProductFormData): Record<string, unknown> {
  const input: Record<string, unknown> = {
    ...values,
    priceCents: yuanToCents(values.price),
    originalPriceCents: values.originalPrice ? yuanToCents(values.originalPrice) : null,
    salesVolume: values.salesVolume || 0,
    sortOrder: values.sortOrder || 0,
  };

  if (values.imageGallery) {
    const gallery = values.imageGallery as string[] | string;
    if (Array.isArray(gallery)) {
      input.imageGallery = JSON.stringify(gallery);
    } else if (typeof gallery === 'string') {
      input.imageGallery = gallery.startsWith('[')
        ? gallery
        : JSON.stringify([gallery]);
    }
  }

  if (values.certificateImages) {
    const certificates = values.certificateImages as string[] | string;
    if (Array.isArray(certificates)) {
      input.certificateImages = JSON.stringify(certificates);
    } else if (typeof certificates === 'string') {
      input.certificateImages = certificates.startsWith('[')
        ? certificates
        : JSON.stringify([certificates]);
    }
  }

  return input;
}
