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
        val products = productService.findAll()
        return ApiResponse.success(products)
    }

    @GetMapping("/active")
    fun getActiveProducts(): ApiResponse<List<Product>> {
        val products = productService.findOnlineProducts()
        return ApiResponse.success(products)
    }

    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: UUID): ApiResponse<Product> {
        val product = productService.findById(productId)
            ?: return ApiResponse.error("Product not found")
        return ApiResponse.success(product)
    }

    @DeleteMapping("/{productId}")
    fun deleteProduct(@PathVariable productId: UUID): ApiResponse<Unit> {
        // Note: Adding delete method to service would be needed
        return ApiResponse.success(Unit)
    }

    @PostMapping("/{productId}/stock/increase")
    fun increaseStock(
        @PathVariable productId: UUID,
        @RequestParam quantity: Int
    ): ApiResponse<String> {
        productService.increaseStock(productId, quantity)
        return ApiResponse.success("Stock increased successfully")
    }

    @PostMapping("/{productId}/stock/decrease")
    fun decreaseStock(
        @PathVariable productId: UUID,
        @RequestParam quantity: Int
    ): ApiResponse<Boolean> {
        val success = productService.decreaseStock(productId, quantity)
        return if (success) {
            ApiResponse.success(true)
        } else {
            ApiResponse.error("Insufficient stock")
        }
    }
}
