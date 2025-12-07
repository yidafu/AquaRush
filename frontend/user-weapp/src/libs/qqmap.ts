import { TENCENT_MAP_KEY } from "@/constants";
import QQMapWX from "./qqmap-wx-jssdk";

// 直接内联一个简化版本的 QQMap SDK 功能
// 避免复杂的模块加载问题

// 创建实例
const qqmap = new QQMapWX({
  key: TENCENT_MAP_KEY
});

export interface IGeocoderOptions {
  address: string;
  province: string;
  city?: string;
  district?: string;
}

export interface IGeocoderResult {
  latitude: number,
  longitude: number,
  address: string,
}

export function geocoder(data: IGeocoderOptions): Promise<IGeocoderResult> {
  let fullAddress = data.province;
  if (data.city) {
    fullAddress += data.city
  }
  if (data.district) {
    fullAddress += data.district
  }
  fullAddress += data.address

  return new Promise((resolve, reject) => {
    qqmap.geocoder({
      address: fullAddress,
      region: data.city,
      success(res) {
        // console.log('geocoder ==> ', res)
        if (res.status === 0) {
          const loc = res.result.location
          // console.log('geocoder ==> ', loc.lat, loc.lng)
          resolve({
            latitude: loc.lat,
            longitude: loc.lng,
            address: res.result.title,
          })
        }
        reject(new Error('Geocoder failed: ' + (res.message || 'Unknown error')))
      },
      fail(err) {
        reject(err)
      }
    })

  })
}
