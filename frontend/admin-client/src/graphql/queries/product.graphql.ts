import { gql } from '@apollo/client';

export const GET_PRODUCTS_QUERY = gql`
  query GetProducts($input: ProductSearchInput) {
    productsPaginated(input: $input) {
      list {
        id
        name
        subtitle
        priceCents
        originalPriceCents
        coverImageUrl
        imageGallery
        specification
        waterSource
        mineralContent
        stock
        salesVolume
        status
        sortOrder
        tags
        detailContent
        certificateImages
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`;

// 库存预警阈值(商品列表着色用,读系统参数 F-SYS-003,缺省 10/50)
// 已并入 PRODUCTS_PAGE_DATA_QUERY(商品列表页单页查询,规则 #10)

/**
 * 商品列表页(ProductListPage)单页查询:
 * 一次请求取回「商品分页 + 库存预警阈值(列表着色)」,满足
 * CLAUDE.md 规则 #10 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const PRODUCTS_PAGE_DATA_QUERY = gql`
  query ProductsPageData($input: ProductSearchInput) {
    productsPaginated(input: $input) {
      list {
        id
        name
        subtitle
        priceCents
        originalPriceCents
        coverImageUrl
        imageGallery
        specification
        waterSource
        mineralContent
        stock
        salesVolume
        status
        sortOrder
        tags
        detailContent
        certificateImages
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
    stockWarningThresholds {
      lowThreshold
      midThreshold
    }
  }
`;

/** ProductsPageData 操作名(供 refetchQueries 引用) */
export const PRODUCTS_PAGE_DATA = 'ProductsPageData';

export const GET_PRODUCT_DETAIL_QUERY = gql`
  query GetProductDetail($id: PrimaryId!) {
    product(id: $id) {
      id
      name
      subtitle
      priceCents
      originalPriceCents
      coverImageUrl
      imageGallery
      specification
      waterSource
      mineralContent
      stock
      salesVolume
      status
      sortOrder
      tags
      detailContent
      certificateImages
      createdAt
      updatedAt
    }
  }
`;

// 商品属性选项字典(规格/水源地下拉候选,F-PRO-009):按类型查询
const PRODUCT_OPTION_FIELDS = gql`
  fragment ProductOptionFields on ProductOption {
    id
    optionType
    value
    sortOrder
    enabled
    createdAt
    updatedAt
  }
`;

// 规格/水源地选项查询(enabledOnly=true 供商品表单,false 供配置页全量展示,规则 #10 单页单请求)
export const PRODUCT_OPTIONS_QUERY = gql`
  query ProductOptions($enabledOnly: Boolean = true) {
    specificationOptions: productOptions(type: SPECIFICATION, enabledOnly: $enabledOnly) {
      ...ProductOptionFields
    }
    waterSourceOptions: productOptions(type: WATER_SOURCE, enabledOnly: $enabledOnly) {
      ...ProductOptionFields
    }
  }
  ${PRODUCT_OPTION_FIELDS}
`;

/** ProductOptions 操作名(供 refetchQueries 引用) */
export const PRODUCT_OPTIONS = 'ProductOptions';

// 商品新增/编辑/详情页单页查询(规则 #10):
// 编辑/详情模式一次请求取回「商品详情 + 规格/水源地启用选项」;
// 新增模式无商品 id,由 useProductFormPageData 改用 PRODUCT_OPTIONS_QUERY(仍为单请求)。
export const PRODUCT_FORM_PAGE_DATA_QUERY = gql`
  query ProductFormPageData($id: PrimaryId!) {
    product(id: $id) {
      id
      name
      subtitle
      priceCents
      originalPriceCents
      coverImageUrl
      imageGallery
      specification
      waterSource
      mineralContent
      stock
      salesVolume
      status
      sortOrder
      tags
      detailContent
      certificateImages
      createdAt
      updatedAt
    }
    specificationOptions: productOptions(type: SPECIFICATION, enabledOnly: true) {
      ...ProductOptionFields
    }
    waterSourceOptions: productOptions(type: WATER_SOURCE, enabledOnly: true) {
      ...ProductOptionFields
    }
  }
  ${PRODUCT_OPTION_FIELDS}
`;

/** ProductFormPageData 操作名(供 refetchQueries 引用) */
export const PRODUCT_FORM_PAGE_DATA = 'ProductFormPageData';
