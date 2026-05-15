package biz.smt_life.android.core.domain.model

data class ItemLocationSearchResult(
    val item: LocationSearchItem,
    val warehouse: LocationSearchWarehouse,
    val stock: LocationSearchStock,
    val locations: LocationSearchLocations
) {
    val displayCode: String
        get() = item.code.ifBlank { item.id.toString() }
}

data class LocationSearchItem(
    val id: Int,
    val code: String,
    val name: String,
    val kana: String? = null,
    val volume: String? = null,
    val volumeUnit: String? = null,
    val capacityCase: Int? = null,
    val capacityCarton: Int? = null,
    val packaging: String? = null,
    val temperatureType: String? = null,
    val usesExpirationDate: Boolean = false,
    val images: List<String> = emptyList(),
    val searchCodes: List<ItemSearchCode> = emptyList(),
    val janCodes: List<String> = emptyList(),
    val itemQuantityCodes: List<ItemQuantityCode> = emptyList()
) {
    val primaryJanCode: String?
        get() = janCodes.firstOrNull() ?: searchCodes.firstOrNull { it.codeType == "JAN" }?.code

    val fullVolume: String?
        get() = if (!volume.isNullOrBlank() && !volumeUnit.isNullOrBlank()) {
            "$volume$volumeUnit"
        } else {
            volume
        }
}

data class ItemSearchCode(
    val code: String,
    val codeType: String? = null,
    val quantityType: String? = null,
    val priority: Int? = null
)

data class ItemQuantityCode(
    val productCode: String? = null,
    val ownCode: String? = null,
    val quantityCode: String? = null,
    val quantity: Int? = null,
    val canOrder: Boolean = false
)

data class LocationSearchWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    val kanaName: String? = null
)

data class LocationSearchStock(
    val status: StockStatus = StockStatus.NO_STOCK,
    val hasStock: Boolean = false,
    val lotCount: Int = 0,
    val locationCount: Int = 0,
    val currentQuantity: Int = 0,
    val reservedQuantity: Int = 0,
    val availableQuantity: Int = 0,
    val earliestExpirationDate: String? = null,
    val latestExpirationDate: String? = null
)

enum class StockStatus {
    IN_STOCK,
    RESERVED_ONLY,
    NO_STOCK,
    UNKNOWN;

    val label: String
        get() = when (this) {
            IN_STOCK -> "在庫あり"
            RESERVED_ONLY -> "引当済"
            NO_STOCK -> "在庫なし"
            UNKNOWN -> "不明"
        }

    companion object {
        fun fromString(value: String?): StockStatus = when (value?.uppercase()) {
            "IN_STOCK" -> IN_STOCK
            "RESERVED_ONLY" -> RESERVED_ONLY
            "NO_STOCK" -> NO_STOCK
            else -> UNKNOWN
        }
    }
}

data class LocationSearchLocations(
    val suggested: ItemLocation? = null,
    val defaultLocation: ItemLocation? = null,
    val stock: List<StockLocation> = emptyList()
)

data class ItemLocation(
    val id: Int,
    val warehouseId: Int,
    val floorId: Int? = null,
    val code: String,
    val displayName: String,
    val name: String? = null,
    val source: String? = null
)

data class StockLocation(
    val id: Int,
    val warehouseId: Int,
    val floorId: Int? = null,
    val code: String,
    val displayName: String,
    val name: String? = null,
    val source: String? = null,
    val lotCount: Int = 0,
    val currentQuantity: Int = 0,
    val reservedQuantity: Int = 0,
    val availableQuantity: Int = 0
)
