package dev.yidafu.aqua.product.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService
) {
    
    @GetMapping
    fun getAllProducts(): ApiResponse<List<Product>> {
        val products = productService.getAllProducts()
        return ApiResponse.success(products)
    }
    
    @GetMapping("/active")
    fun getActiveProducts(): ApiResponse<List<Product>> {
        val products = productService.getActiveProducts()
        return ApiResponse.success(products)
    }
    
    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: UUID): ApiResponse<Product> {
        val product = productService.getProductById(productId)
        return ApiResponse.success(product)
    }
    
    @PostMapping
    fun createProduct(@RequestBody product: Product): ApiResponse<Product> {
        val createdProduct = productService.createProduct(product)
        return ApiResponse.success(createdProduct)
    }
    
    @PutMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: UUID,
        @RequestBody product: Product
    ): ApiResponse<Product> {
        val updatedProduct = productService.updateProduct(productId, product)
        return ApiResponse.success(updatedProduct)
    }
    
    @DeleteMapping("/{productId}")
    fun deleteProduct(@PathVariable productId: UUID): ApiResponse<Unit> {
        productService.deleteProduct(productId)
        return ApiResponse.success(Unit)
    }
    
    @PostMapping("/{productId}/stock")
    fun updateStock(
        @PathVariable productId: UUID,
        @RequestParam quantity: Int
    ): ApiResponse<Product> {
        val product = productService.updateStock(productId, quantity)
        return ApiResponse.success(product)
    }
}
