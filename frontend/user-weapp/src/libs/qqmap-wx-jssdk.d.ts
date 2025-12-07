/**
 * TypeScript declarations for QQMapWX WeChat Mini Program JavaScript SDK
 * @version 1.2
 */

  /**
   * 基础位置信息接口
   */
  export interface Location {
    lat: number;
    lng: number;
  }

  /**
   * 带位置信息的接口
   */
  export interface LocationPoint {
    location: Location;
  }

  /**
   * 带经纬度的接口
   */
  export interface LatLngPoint {
    latitude: number;
    longitude: number;
  }

  /**
   * 广告信息接口
   */
  export interface AdInfo {
    adcode: string;
    city: string;
    district: string;
    province: string;
  }

  /**
   * 地址组件信息
   */
  export interface AddressComponent {
    city: string;
    district: string;
    nation: string;
    province: string;
    street: string;
    street_number: string;
  }

  /**
   * 格式化地址信息
   */
  export interface FormattedAddresses {
    recommend: string;
    rough: string;
  }

  /**
   * POI信息接口
   */
  export interface POI {
    id: string;
    title: string;
    address: string;
    category: string;
    tel?: string;
    location: Location;
    ad_info: AdInfo;
  }

  /**
   * 简化的POI信息接口
   */
  export interface SimplifiedPOI {
    id: string | null;
    title: string | null;
    latitude: number | null;
    longitude: number | null;
    address: string | null;
    category: string | null;
    tel: string | null;
    adcode: string | null;
    city: string | null;
    district: string | null;
    province: string | null;
  }

  /**
   * 搜索结果项
   */
  export interface SearchResultItem {
    id: string;
    title: string;
    address?: string;
    category?: string;
    tel?: string;
    location: Location;
    ad_info: AdInfo;
  }

  /**
   * 建议结果项
   */
  export interface SuggestionResultItem {
    id: string;
    title: string;
    address: string;
    category: string;
    city: string;
    district: string;
    province: string;
    adcode: string;
    type: string;
    location: Location;
  }

  /**
   * 逆地理编码结果
   */
  export interface ReverseGeocoderResult {
    address: string;
    location: Location;
    ad_info: AdInfo;
    address_component: AddressComponent;
    formatted_addresses: FormattedAddresses;
    pois?: POI[];
  }

  /**
   * 逆地理编码简化结果
   */
  export interface ReverseGeocoderSimplify {
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    adcode: string | null;
    city: string | null;
    district: string | null;
    nation: string | null;
    province: string | null;
    street: string | null;
    street_number: string | null;
    recommend: string | null;
    rough: string | null;
  }

  /**
   * 地理编码结果
   */
  export interface GeocoderResult {
    title: string;
    location: Location;
    ad_info: AdInfo;
    address_components: AddressComponent;
    level: string;
  }

  /**
   * 地理编码简化结果
   */
  export interface GeocoderSimplify {
    title: string | null;
    latitude: number | null;
    longitude: number | null;
    adcode: string | null;
    province: string | null;
    city: string | null;
    district: string | null;
    street: string | null;
    street_number: string | null;
    level: string | null;
  }

  /**
   * 距离计算元素
   */
  export interface DistanceElement {
    distance: number;
    duration: number;
    from: Location;
    to: Location;
  }

  /**
   * 距离计算结果
   */
  export interface CalculateDistanceResult {
    elements: DistanceElement[];
  }

  /**
   * 基础响应接口
   */
  export interface BaseResponse {
    status: number;
    message: string;
  }

  /**
   * 搜索响应数据
   */
  export interface SearchResponseData extends BaseResponse {
    data: SearchResultItem[];
  }

  /**
   * 建议响应数据
   */
  export interface SuggestionResponseData extends BaseResponse {
    data: SuggestionResultItem[];
  }

  /**
   * 逆地理编码响应数据
   */
  export interface ReverseGeocoderResponseData extends BaseResponse {
    result: ReverseGeocoderResult;
  }

  /**
   * 地理编码响应数据
   */
  export interface GeocoderResponseData extends BaseResponse {
    result: GeocoderResult;
  }

  /**
   * 城市列表响应数据
   */
  export interface CityListResponseData extends BaseResponse {
    result: [any[], any[], any[]];
  }

  /**
   * 区县列表响应数据
   */
  export interface DistrictByCityResponseData extends BaseResponse {
    result: any[];
  }

  /**
   * 距离计算响应数据
   */
  export interface CalculateDistanceResponseData extends BaseResponse {
    result: CalculateDistanceResult;
  }

  /**
   * 路线规划响应数据
   */
  export interface DirectionResponseData extends BaseResponse {
    result: {
      routes: any[];
    };
  }

  /**
   * 搜索方法成功回调
   */
  export type SearchSuccessCallback = (
    data: SearchResponseData,
    result: {
      searchResult: SearchResultItem[];
      searchSimplify: SimplifiedPOI[];
    }
  ) => void;

  /**
   * 建议方法成功回调
   */
  export type SuggestionSuccessCallback = (
    data: SuggestionResponseData,
    result: {
      suggestResult: SuggestionResultItem[];
      suggestSimplify: SimplifiedPOI[];
    }
  ) => void;

  /**
   * 逆地理编码成功回调
   */
  export type ReverseGeocoderSuccessCallback = (
    data: ReverseGeocoderResponseData,
    result: {
      reverseGeocoderResult: ReverseGeocoderResult;
      reverseGeocoderSimplify: ReverseGeocoderSimplify;
      pois?: POI[];
      poisSimplify?: SimplifiedPOI[];
    }
  ) => void;

  /**
   * 地理编码成功回调
   */
  export type GeocoderSuccessCallback = (
    data: GeocoderResponseData,
    result: {
      geocoderResult: GeocoderResult;
      geocoderSimplify: GeocoderSimplify;
    }
  ) => void;

  /**
   * 城市列表成功回调
   */
  export type CityListSuccessCallback = (
    data: CityListResponseData,
    result: {
      provinceResult: any[];
      cityResult: any[];
      districtResult: any[];
    }
  ) => void;

  /**
   * 区县列表成功回调
   */
  export type DistrictByCitySuccessCallback = (
    data: DistrictByCityResponseData,
    result: any[]
  ) => void;

  /**
   * 距离计算成功回调
   */
  export type CalculateDistanceSuccessCallback = (
    data: CalculateDistanceResponseData,
    result: {
      calculateDistanceResult: DistanceElement[];
      distance: number[];
    }
  ) => void;

  /**
   * 路线规划成功回调
   */
  export type DirectionSuccessCallback = (
    data: DirectionResponseData,
    result: any[]
  ) => void;

  /**
   * 通用失败回调
   */
  export type FailCallback = (error: {
    status: number;
    message: string;
  }) => void;

  /**
   * 通用完成回调
   */
  export type CompleteCallback = (response: any) => void;

  /**
   * 搜索方法参数
   */
  export interface SearchOptions {
    /** 检索关键词 */
    keyword: string;
    /** 检索中心点，可选 */
    location?: string | Location;
    /** 检索半径，默认1000米 */
    distance?: string | number;
    /** 是否自动扩大范围，默认1 */
    auto_extend?: number;
    /** 检索排序方式，默认按距离排序 */
    orderby?: string;
    /** 返回第几页结果，默认1 */
    page_index?: number;
    /** 每页显示结果数量，默认10 */
    page_size?: number;
    /** 城市限定 */
    region?: string;
    /** 矩形区域限定 */
    rectangle?: string;
    /** 过滤条件 */
    filter?: string;
    /** 地址格式 */
    address_format?: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: SearchSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 建议方法参数
   */
  export interface SuggestionOptions {
    /** 检索关键词 */
    keyword: string;
    /** 限定城市，默认全国 */
    region?: string;
    /** 限定城市返回结果，默认0 */
    region_fix?: number;
    /** 检索策略，默认0 */
    policy?: number;
    /** 位置坐标，用于排序 */
    location?: string | Location;
    /** 返回子地点，默认0 */
    get_subpois?: number;
    /** 返回第几页结果，默认1 */
    page_index?: number;
    /** 每页显示结果数量，默认10 */
    page_size?: number;
    /** 过滤条件 */
    filter?: string;
    /** 地址格式 */
    address_format?: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: SuggestionSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 逆地理编码方法参数
   */
  export interface ReverseGeocoderOptions {
    /** 位置坐标 */
    location?: string | Location;
    /** 坐标类型，默认5 */
    coord_type?: number;
    /** 是否返回周边POI，默认0 */
    get_poi?: number;
    /** POI控制参数 */
    poi_options?: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: ReverseGeocoderSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 地理编码方法参数
   */
  export interface GeocoderOptions {
    /** 地址描述 */
    address: string;
    /** 限定城市 */
    region?: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: GeocoderSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 城市列表方法参数
   */
  export interface CityListOptions {
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: CityListSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 区县列表方法参数
   */
  export interface DistrictByCityOptions {
    /** 城市ID */
    id: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: DistrictByCitySuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 距离计算方法参数
   */
  export interface CalculateDistanceOptions {
    /** 终点位置，可以是字符串或数组 */
    to: string | (LocationPoint | LatLngPoint)[];
    /** 起点位置 */
    from?: string | Location;
    /** 计算方式：walking、driving、straight */
    mode?: 'walking' | 'driving' | 'straight';
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: CalculateDistanceSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * 路线规划方法参数
   */
  export interface DirectionOptions {
    /** 终点位置 */
    to: string | Location;
    /** 起点位置 */
    from?: string | Location;
    /** 路线规划方式：driving、transit */
    mode?: 'driving' | 'transit';
    /** 驾车策略 */
    policy?: string;
    /** 避让区域 */
    avoidpolygons?: string;
    /** 避让道路 */
    avoidroad?: string;
    /** 车牌号 */
    plate_number?: string;
    /** 出发时间 */
    departure_time?: string;
    /** 签名 */
    sig?: string;
    /** 成功回调 */
    success?: DirectionSuccessCallback;
    /** 失败回调 */
    fail?: FailCallback;
    /** 完成回调 */
    complete?: CompleteCallback;
  }

  /**
   * QQMapWX构造函数参数
   */
  export interface QQMapWXOptions {
    /** 腾讯地图开发者密钥 */
    key: string;
  }

  /**
   * QQMapWX主类
   */
  export class QQMapWX {
    constructor(options: QQMapWXOptions);

    /**
     * POI周边检索
     */
    search(options: SearchOptions): void;

    /**
     * 关键词模糊检索
     */
    getSuggestion(options: SuggestionOptions): void;

    /**
     * 逆地址解析（坐标转地址）
     */
    reverseGeocoder(options: ReverseGeocoderOptions): void;

    /**
     * 地址解析（地址转坐标）
     */
    geocoder(options: GeocoderOptions): void;

    /**
     * 获取城市列表
     */
    getCityList(options: CityListOptions): void;

    /**
     * 获取对应城市ID的区县列表
     */
    getDistrictByCityId(options: DistrictByCityOptions): void;

    /**
     * 距离计算
     */
    calculateDistance(options: CalculateDistanceOptions): void;

    /**
     * 路线规划
     */
    direction(options: DirectionOptions): void;
  }

  export default QQMapWX;
