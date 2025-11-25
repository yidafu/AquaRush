package dev.yidafu.aqua.delivery.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.delivery.domain.model.DeliveryArea
import dev.yidafu.aqua.delivery.domain.model.DeliveryWorker
import dev.yidafu.aqua.delivery.domain.model.WorkerStatus
import dev.yidafu.aqua.delivery.service.DeliveryService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/delivery")
class DeliveryController(
    private val deliveryService: DeliveryService
) {
    
    // 配送员接口
    
    @GetMapping("/workers")
    fun getAllWorkers(): ApiResponse<List<DeliveryWorker>> {
        val workers = deliveryService.getAllWorkers()
        return ApiResponse.success(workers)
    }
    
    @GetMapping("/workers/online")
    fun getOnlineWorkers(): ApiResponse<List<DeliveryWorker>> {
        val workers = deliveryService.getOnlineWorkers()
        return ApiResponse.success(workers)
    }
    
    @GetMapping("/workers/{workerId}")
    fun getWorker(@PathVariable workerId: UUID): ApiResponse<DeliveryWorker> {
        val worker = deliveryService.getWorkerById(workerId)
        return ApiResponse.success(worker)
    }
    
    @PostMapping("/workers/{workerId}/status")
    fun updateWorkerStatus(
        @PathVariable workerId: UUID,
        @RequestParam status: WorkerStatus
    ): ApiResponse<DeliveryWorker> {
        val worker = deliveryService.updateWorkerStatus(workerId, status)
        return ApiResponse.success(worker)
    }
    
    // 配送区域接口
    
    @GetMapping("/areas")
    fun getAllAreas(): ApiResponse<List<DeliveryArea>> {
        val areas = deliveryService.getAllDeliveryAreas()
        return ApiResponse.success(areas)
    }
    
    @GetMapping("/areas/enabled")
    fun getEnabledAreas(): ApiResponse<List<DeliveryArea>> {
        val areas = deliveryService.getEnabledDeliveryAreas()
        return ApiResponse.success(areas)
    }
    
    @PostMapping("/areas")
    fun createArea(@RequestBody area: DeliveryArea): ApiResponse<DeliveryArea> {
        val createdArea = deliveryService.createDeliveryArea(area)
        return ApiResponse.success(createdArea)
    }
    
    @PostMapping("/areas/validate")
    fun validateAddress(
        @RequestParam province: String,
        @RequestParam city: String,
        @RequestParam district: String
    ): ApiResponse<Boolean> {
        val isValid = deliveryService.isAddressInDeliveryArea(province, city, district)
        return ApiResponse.success(isValid)
    }
}
