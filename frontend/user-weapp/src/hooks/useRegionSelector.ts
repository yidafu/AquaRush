import { useState, useEffect, useCallback, useRef } from 'react'
import Taro from '@tarojs/taro'
import RegionService from '../services/RegionService'
import {
  Region,
  RegionSelection,
  RegionLoadingState,
  RegionErrorState,
  RegionHierarchy
} from '../types/region'

interface UseRegionSelectorOptions {
  initialSelection?: {
    provinceCode?: string
    cityCode?: string
    districtCode?: string
  }
  autoLoadProvinces?: boolean
  onError?: (error: string) => void
}

interface UseRegionSelectorReturn {
  // Data
  provinces: Region[]
  cities: Region[]
  districts: Region[]

  // Selected items
  selectedProvince: Region | null
  selectedCity: Region | null
  selectedDistrict: Region | null

  // Selected indices for picker
  selectedIndex: {
    province: number
    city: number
    district: number
  }

  // Loading states
  loading: RegionLoadingState

  // Error states
  errors: RegionErrorState

  // Handlers
  handleColumnChange: (event: any) => Promise<void>
  handlePickerChange: (event: any) => void

  // Utility methods
  getFullRegionText: () => string
  resetSelection: () => void
  getSelectedCodes: () => {
    provinceCode?: string
    cityCode?: string
    districtCode?: string
  }
  getSelectedNames: () => {
    province?: string
    city?: string
    district?: string
  }
}
const regionService = RegionService.getInstance()

export const useRegionSelector = (options: UseRegionSelectorOptions = {}): UseRegionSelectorReturn => {
  const {
    initialSelection,
    autoLoadProvinces = true,
    onError
  } = options

  // Data state
  const [provinces, setProvinces] = useState<Region[]>([])
  const [cities, setCities] = useState<Region[]>([])
  const [districts, setDistricts] = useState<Region[]>([])

  // Selected items state
  const [selectedProvince, setSelectedProvince] = useState<Region | null>(null)
  const [selectedCity, setSelectedCity] = useState<Region | null>(null)
  const [selectedDistrict, setSelectedDistrict] = useState<Region | null>(null)

  // Selected indices state
  const [selectedIndex, setSelectedIndex] = useState({
    province: 0,
    city: 0,
    district: 0
  })

  // Loading state
  const [loading, setLoading] = useState<RegionLoadingState>({
    provinces: false,
    cities: false,
    districts: false
  })

  // Error state
  const [errors, setErrors] = useState<RegionErrorState>({})

  // Refs for managing async operations
  const loadingRef = useRef<{
    provinces: boolean
    cities: boolean
    districts: boolean
  }>({
    provinces: false,
    cities: false,
    districts: false
  })

  // Initialize region service

  // Load provinces
  const loadProvinces = useCallback(async () => {
    // Use ref to track if provinces are already loaded to avoid dependency issues
    if (loadingRef.current.provinces && provinces.length > 0) return

    setLoading(prev => ({ ...prev, provinces: true }))
    setErrors(prev => ({ ...prev, provinces: undefined }))

    try {
      const provincesData = await regionService.getProvinces()
      setProvinces(provincesData)
      loadingRef.current.provinces = true // Mark as loaded

      // Auto-select first province if none selected
      setSelectedProvince(prev => {
        if (!prev && provincesData.length > 0) {
          return provincesData[0]
        }
        return prev
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load provinces'
      setErrors(prev => ({ ...prev, provinces: errorMessage }))
      onError?.(errorMessage)
    } finally {
      setLoading(prev => ({ ...prev, provinces: false }))
    }
  }, [provinces.length, onError])

  // Load cities for a province
  const loadCities = useCallback(async (province: Region) => {
    if (!province.code) return

    // Prevent duplicate requests
    if (loadingRef.current.cities) return
    loadingRef.current.cities = true

    setLoading(prev => ({ ...prev, cities: true }))
    setErrors(prev => ({ ...prev, cities: undefined }))

    try {
      const citiesData = await regionService.getCities(province.code)
      setCities(citiesData)

      // Auto-select first city if none selected
      if (citiesData.length > 0) {
        setSelectedCity(citiesData[0])
        setSelectedIndex(prev => ({ ...prev, city: 0 }))
      } else {
        setSelectedCity(null)
        setSelectedDistrict(null)
        setDistricts([])
        setSelectedIndex(prev => ({ ...prev, city: 0, district: 0 }))
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load cities'
      setErrors(prev => ({ ...prev, cities: errorMessage }))
      onError?.(errorMessage)
    } finally {
      setLoading(prev => ({ ...prev, cities: false }))
      loadingRef.current.cities = false
    }
  }, [onError])

  // Load districts for a city
  const loadDistricts = useCallback(async (city: Region) => {
    if (!city.code) return

    // Prevent duplicate requests
    if (loadingRef.current.districts) return
    loadingRef.current.districts = true

    setLoading(prev => ({ ...prev, districts: true }))
    setErrors(prev => ({ ...prev, districts: undefined }))

    try {
      const districtsData = await regionService.getDistricts(city.code)
      setDistricts(districtsData)

      // Auto-select first district if none selected
      if (districtsData.length > 0) {
        setSelectedDistrict(districtsData[0])
        setSelectedIndex(prev => ({ ...prev, district: 0 }))
      } else {
        setSelectedDistrict(null)
        setSelectedIndex(prev => ({ ...prev, district: 0 }))
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load districts'
      setErrors(prev => ({ ...prev, districts: errorMessage }))
      onError?.(errorMessage)
    } finally {
      setLoading(prev => ({ ...prev, districts: false }))
      loadingRef.current.districts = false
    }
  }, [onError])

  // Load backend default region hierarchy
  const loadBackendDefault = useCallback(async () => {
    setLoading(prev => ({ ...prev, provinces: true }))
    setErrors(prev => ({ ...prev, provinces: undefined }))

    try {
      const defaultHierarchy = await regionService.getDefaultRegionHierarchy()
      if (defaultHierarchy) {
        // Set data from backend default
        setProvinces(defaultHierarchy.provinces)
        setCities(defaultHierarchy.cities)
        setDistricts(defaultHierarchy.districts)

        // Set selected regions
        setSelectedProvince(defaultHierarchy.province)
        setSelectedCity(defaultHierarchy.city)
        setSelectedDistrict(defaultHierarchy.district)

        // Calculate and set selected indices
        const provinceIndex = defaultHierarchy.provinces.findIndex(p => p.code === defaultHierarchy.province.code)
        const cityIndex = defaultHierarchy.cities.findIndex(c => c.code === defaultHierarchy.city.code)
        const districtIndex = defaultHierarchy.districts.findIndex(d => d.code === defaultHierarchy.district.code)

        setSelectedIndex({
          province: Math.max(0, provinceIndex),
          city: Math.max(0, cityIndex),
          district: Math.max(0, districtIndex)
        })

        loadingRef.current.provinces = true
      } else {
        // Fallback to regular load if backend default fails
        console.warn('No backend default region hierarchy found, falling back to regular load')
        const provincesData = await regionService.getProvinces()
        setProvinces(provincesData)
        loadingRef.current.provinces = true
        setSelectedProvince(provincesData[0] || null)
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load backend default region'
      console.error('Backend default region load failed:', error)
      setErrors(prev => ({ ...prev, provinces: errorMessage }))
      onError?.(errorMessage)

      // Fallback to regular load
      try {
        const provincesData = await regionService.getProvinces()
        setProvinces(provincesData)
        loadingRef.current.provinces = true
        setSelectedProvince(provincesData[0] || null)
      } catch (fallbackError) {
        console.error('Fallback load also failed:', fallbackError)
      }
    } finally {
      setLoading(prev => ({ ...prev, provinces: false }))
    }
  }, [onError])

  // Initialize with initial selection
  const initializeWithSelection = useCallback(async () => {
    if (!initialSelection) return

    try {
      const { provinceCode, cityCode, districtCode } = initialSelection

      // Load provinces first
      await loadProvinces()

      // Get fresh data after loading
      const provincesData = await regionService.getProvinces()

      // Set initial province
      if (provinceCode) {
        const province = provincesData.find(p => p.code === provinceCode)
        if (province) {
          setSelectedProvince(province)
          const provinceIndex = provincesData.indexOf(province)
          setSelectedIndex(prev => ({ ...prev, province: Math.max(0, provinceIndex) }))

          // Load cities for this province
          await loadCities(province)
          const citiesData = await regionService.getCities(province.code)

          // Set initial city
          if (cityCode) {
            const city = citiesData.find(c => c.code === cityCode)
            if (city) {
              setSelectedCity(city)
              const cityIndex = citiesData.indexOf(city)
              setSelectedIndex(prev => ({ ...prev, city: Math.max(0, cityIndex) }))

              // Load districts for this city
              await loadDistricts(city)
              const districtsData = await regionService.getDistricts(city.code)

              // Set initial district
              if (districtCode) {
                const district = districtsData.find(d => d.code === districtCode)
                if (district) {
                  setSelectedDistrict(district)
                  const districtIndex = districtsData.indexOf(district)
                  setSelectedIndex(prev => ({ ...prev, district: Math.max(0, districtIndex) }))
                }
              }
            }
          }
        }
      }
    } catch (error) {
      console.error('Failed to initialize with selection:', error)
    }
  }, [initialSelection, loadProvinces, loadCities, loadDistricts])

  // Handle column change for cascading picker
  const handleColumnChange = useCallback(async (event: any) => {
    const { column, value } = event.detail

    try {
      if (column === 0) {
        // Province changed
        const province = provinces[value]
        if (province && province !== selectedProvince) {
          setSelectedProvince(province)
          setSelectedIndex(prev => ({ ...prev, province: value, city: 0, district: 0 }))

          // Reset city and district
          setSelectedCity(null)
          setSelectedDistrict(null)
          setCities([])
          setDistricts([])

          // Load cities for new province
          await loadCities(province)
        }
      } else if (column === 1) {
        // City changed
        const city = cities[value]
        if (city && city !== selectedCity) {
          setSelectedCity(city)
          setSelectedIndex(prev => ({ ...prev, city: value, district: 0 }))

          // Reset district
          setSelectedDistrict(null)
          setDistricts([])

          // Load districts for new city
          await loadDistricts(city)
        }
      } else if (column === 2) {
        // District changed
        const district = districts[value]
        if (district) {
          setSelectedDistrict(district)
          setSelectedIndex(prev => ({ ...prev, district: value }))
        }
      }
    } catch (error) {
      console.error('Error handling column change:', error)
      const errorMessage = error instanceof Error ? error.message : 'Failed to update region selection'
      onError?.(errorMessage)
    }
  }, [provinces, cities, districts, selectedProvince, selectedCity, loadCities, loadDistricts, onError])

  // Handle picker final change
  const handlePickerChange = useCallback((event: any) => {
    const { value } = event.detail
    const [provinceIndex, cityIndex, districtIndex] = value

    setSelectedIndex({
      province: provinceIndex,
      city: cityIndex,
      district: districtIndex
    })

    // Update selected items based on indices
    if (provinces[provinceIndex]) {
      setSelectedProvince(provinces[provinceIndex])
    }

    if (cities[cityIndex]) {
      setSelectedCity(cities[cityIndex])
    }

    if (districts[districtIndex]) {
      setSelectedDistrict(districts[districtIndex])
    }
  }, [provinces, cities, districts])

  // Get full region text for display
  const getFullRegionText = useCallback(() => {
    const parts = []
    if (selectedProvince) parts.push(selectedProvince.name)
    if (selectedCity) parts.push(selectedCity.name)
    if (selectedDistrict) parts.push(selectedDistrict.name)
    return parts.join(' ')
  }, [selectedProvince, selectedCity, selectedDistrict])

  // Reset selection
  const resetSelection = useCallback(() => {
    setSelectedProvince(provinces[0] || null)
    setSelectedCity(null)
    setSelectedDistrict(null)
    setCities([])
    setDistricts([])
    setSelectedIndex({
      province: 0,
      city: 0,
      district: 0
    })
    setErrors({})
  }, [provinces])

  // Get selected codes
  const getSelectedCodes = useCallback(() => ({
    provinceCode: selectedProvince?.code,
    cityCode: selectedCity?.code,
    districtCode: selectedDistrict?.code
  }), [selectedProvince, selectedCity, selectedDistrict])

  // Get selected names
  const getSelectedNames = useCallback(() => ({
    province: selectedProvince?.name,
    city: selectedCity?.name,
    district: selectedDistrict?.name
  }), [selectedProvince, selectedCity, selectedDistrict])

  // Ref to track if initialization has been done
  const initializedRef = useRef(false)

  // Initialize component
  useEffect(() => {
    if (!initializedRef.current && autoLoadProvinces) {
      initializedRef.current = true

      if (initialSelection) {
        initializeWithSelection()
      } else {
        loadBackendDefault()
      }
    }
  }, [autoLoadProvinces, initialSelection, loadBackendDefault, initializeWithSelection])

  return {
    // Data
    provinces,
    cities,
    districts,

    // Selected items
    selectedProvince,
    selectedCity,
    selectedDistrict,

    // Selected indices
    selectedIndex,

    // Loading states
    loading,

    // Error states
    errors,

    // Handlers
    handleColumnChange,
    handlePickerChange,

    // Utility methods
    getFullRegionText,
    resetSelection,
    getSelectedCodes,
    getSelectedNames
  }
}
