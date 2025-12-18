import React, { createContext, useContext, ReactNode } from 'react'
import { Product } from '@/types/product'

interface ProductContextType {
  product: Product
}

const ProductContext = createContext<ProductContextType | undefined>(undefined)

interface ProductProviderProps {
  children: ReactNode
  product: Product
}

export const ProductProvider: React.FC<ProductProviderProps> = ({
  children,
  product
}) => {
  const contextValue: ProductContextType = {
    product
  }

  return (
    <ProductContext.Provider value={contextValue}>
      {children}
    </ProductContext.Provider>
  )
}

export const useProduct = (): ProductContextType => {
  const context = useContext(ProductContext)
  if (!context) {
    throw new Error('useProduct must be used within a ProductProvider')
  }
  return context
}

export default ProductContext
