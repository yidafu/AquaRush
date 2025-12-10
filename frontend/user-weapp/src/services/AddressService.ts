import NetworkManager from '../utils/network'
import {
  AddressInput,
  Address,
  CreateAddressResponse,
  UpdateAddressResponse,
  DeleteAddressResponse,
  SetDefaultAddressResponse,
  UserAddressesResponse,
  AddressServiceResult,
  AddressServiceError
} from '../types/address'

class AddressService {
  private static instance: AddressService
  private networkManager: NetworkManager

  private constructor() {
    // Use the same configuration as the region service
    this.networkManager = NetworkManager.getInstance({
      baseURL: 'http://localhost:8080/graphql',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })
  }

  public static getInstance(): AddressService {
    if (!AddressService.instance) {
      AddressService.instance = new AddressService()
    }
    return AddressService.instance
  }

  /**
   * Get all addresses for the current user
   * @returns Promise with address list or error
   */
  public async getUserAddresses(): Promise<AddressServiceResult<Address[]>> {
    try {
      const query = `
        query UserAddresses {
          userAddresses {
            id
            receiverName
            phone
            province
            city
            district
            detailAddress
            longitude
            latitude
            isDefault
          }
        }
      `

      console.log('Fetching user addresses...')

      const response = await this.networkManager.query<UserAddressesResponse>(query, {})

      if (!response?.userAddresses) {
        throw new Error('Failed to fetch addresses: No data returned')
      }

      console.log('User addresses fetched successfully:', response.userAddresses)

      return {
        success: true,
        data: response.userAddresses
      }
    } catch (error) {
      console.error('Get user addresses error:', error)

      let errorMessage = '获取地址列表失败，请重试'
      let errorCode: string | undefined

      // Handle GraphQL errors
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          const firstError = graphqlErrors[0]
          errorMessage = firstError.message || errorMessage
          errorCode = firstError.extensions?.code
        }
      } else if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage,
          code: errorCode
        }
      }
    }
  }

  /**
   * Create a new address
   * @param addressData Address data to create
   * @returns Promise with created address or error
   */
  public async createAddress(addressData: AddressInput): Promise<AddressServiceResult<Address>> {
    try {
      const mutation = `
        mutation CreateAddress($input: AddressInput!) {
          createAddress(input: $input) {
            id
            userId
            receiverName
            phone
            province
            provinceCode
            city
            cityCode
            district
            districtCode
            detailAddress
            longitude
            latitude
            isDefault
            createdAt
            updatedAt
          }
        }
      `

      const variables = {
        input: addressData
      }

      console.log('Creating address with data:', addressData)

      const response = await this.networkManager.mutate<CreateAddressResponse>(mutation, variables, {})

      if (!response?.createAddress) {
        throw new Error('Failed to create address: No data returned')
      }

      console.log('Address created successfully:', response.createAddress)

      return {
        success: true,
        data: response.createAddress
      }
    } catch (error) {
      console.error('Create address error:', error)

      let errorMessage = '创建地址失败，请重试'
      let errorCode: string | undefined
      let errorField: string | undefined

      // Handle GraphQL errors
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          const firstError = graphqlErrors[0]
          errorMessage = firstError.message || errorMessage

          // Extract field information if available
          if (firstError.path && Array.isArray(firstError.path)) {
            errorField = firstError.path[0] as string
          }

          errorCode = firstError.extensions?.code
        }
      } else if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage,
          code: errorCode,
          field: errorField
        }
      }
    }
  }

  /**
   * Update an existing address
   * @param id Address ID to update
   * @param addressData Updated address data
   * @returns Promise with updated address or error
   */
  public async updateAddress(id: number, addressData: AddressInput): Promise<AddressServiceResult<Address>> {
    try {
      const mutation = `
        mutation UpdateAddress($id: Long!, $input: AddressInput!) {
          updateAddress(id: $id, input: $input) {
            id
            userId
            receiverName
            phone
            province
            provinceCode
            city
            cityCode
            district
            districtCode
            detailAddress
            longitude
            latitude
            isDefault
            createdAt
            updatedAt
          }
        }
      `

      const variables = {
        id,
        input: addressData
      }

      console.log('Updating address with data:', { id, ...addressData })

      const response = await this.networkManager.mutate<UpdateAddressResponse>(mutation, variables, {})

      if (!response?.updateAddress) {
        throw new Error('Failed to update address: No data returned')
      }

      console.log('Address updated successfully:', response.updateAddress)

      return {
        success: true,
        data: response.updateAddress
      }
    } catch (error) {
      console.error('Update address error:', error)

      let errorMessage = '更新地址失败，请重试'
      let errorCode: string | undefined
      let errorField: string | undefined

      // Handle GraphQL errors
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          const firstError = graphqlErrors[0]
          errorMessage = firstError.message || errorMessage

          if (firstError.path && Array.isArray(firstError.path)) {
            errorField = firstError.path[0] as string
          }

          errorCode = firstError.extensions?.code
        }
      } else if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage,
          code: errorCode,
          field: errorField
        }
      }
    }
  }

  /**
   * Delete an address
   * @param id Address ID to delete
   * @returns Promise with deletion result or error
   */
  public async deleteAddress(id: number): Promise<AddressServiceResult<boolean>> {
    try {
      const mutation = `
        mutation DeleteAddress($id: Long!) {
          deleteAddress(id: $id)
        }
      `

      const variables = { id }

      console.log('Deleting address:', id)

      const response = await this.networkManager.mutate<DeleteAddressResponse>(mutation, variables, {})

      if (response?.deleteAddress === undefined) {
        throw new Error('Failed to delete address: No response')
      }

      console.log('Address deleted successfully:', id)

      return {
        success: true,
        data: response.deleteAddress
      }
    } catch (error) {
      console.error('Delete address error:', error)

      let errorMessage = '删除地址失败，请重试'

      if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage
        }
      }
    }
  }

  /**
   * Set address as default
   * @param id Address ID to set as default
   * @returns Promise with operation result or error
   */
  public async setDefaultAddress(id: number): Promise<AddressServiceResult<boolean>> {
    try {
      const mutation = `
        mutation SetDefaultAddress($id: Long!) {
          setDefaultAddress(id: $id)
        }
      `

      const variables = { id }

      console.log('Setting address as default:', id)

      const response = await this.networkManager.mutate<SetDefaultAddressResponse>(mutation, variables, {})

      if (response?.setDefaultAddress === undefined) {
        throw new Error('Failed to set default address: No response')
      }

      console.log('Address set as default successfully:', id)

      return {
        success: true,
        data: response.setDefaultAddress
      }
    } catch (error) {
      console.error('Set default address error:', error)

      let errorMessage = '设置默认地址失败，请重试'

      if (error instanceof Error) {
        errorMessage = error.message
      }

      return {
        success: false,
        error: {
          message: errorMessage
        }
      }
    }
  }
}

export default AddressService