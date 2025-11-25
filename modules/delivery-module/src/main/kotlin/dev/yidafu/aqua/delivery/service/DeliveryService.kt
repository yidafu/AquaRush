package dev.yidafu.aqua.delivery.service

import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.model.DeliveryArea
import dev.yidafu.aqua.delivery.domain.model.DeliveryWorker
import dev.yidafu.aqua.delivery.domain.model.WorkerStatus
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeliveryService(
    private val workerRepository: DeliveryWorkerRepository,
    private val areaRepository: DeliveryAreaRepository
) {
    
    // 配送员管理
    
    fun getWorkerById(workerId: UUID): DeliveryWorker {
        return workerRepository.findById(workerId).orElseThrow {
            NotFoundException("配送员不存在: $workerId")
        }
    }
    
    fun getAllWorkers(): List<DeliveryWorker> {
        return workerRepository.findAll()
    }
    
    fun getOnlineWorkers(): List<DeliveryWorker> {
        return workerRepository.findByStatus(WorkerStatus.ONLINE)
    }

    @Transactional
    fun updateWorkerStatus(workerId: UUID, status: WorkerStatus): DeliveryWorker {
        val worker = getWorkerById(workerId)
        worker.status = status
        return workerRepository.save(worker)
    }
    
    // 配送区域管理
    
    fun isAddressInDeliveryArea(province: String, city: String, district: String): Boolean {
        val area = areaRepository.findByProvinceAndCityAndDistrict(province, city, district)
        return area != null && area.enabled
    }
    
    fun validateDeliveryAddress(province: String, city: String, district: String) {
        if (!isAddressInDeliveryArea(province, city, district)) {
            throw BadRequestException("该地址不在配送范围内")
        }
    }
    
    fun getAllDeliveryAreas(): List<DeliveryArea> {
        return areaRepository.findAll()
    }
    
    fun getEnabledDeliveryAreas(): List<DeliveryArea> {
        return areaRepository.findByEnabledTrue()
    }
    
    @Transactional
    fun createDeliveryArea(area: DeliveryArea): DeliveryArea {
        return areaRepository.save(area)
    }
    
    @Transactional
    fun updateDeliveryArea(areaId: UUID, enabled: Boolean): DeliveryArea {
        val area = areaRepository.findById(areaId).orElseThrow {
            NotFoundException("配送区域不存在: $areaId")
        }
        area.enabled = enabled
        return areaRepository.save(area)
    }
}
