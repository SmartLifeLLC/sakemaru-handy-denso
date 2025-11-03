package biz.smt_life.android.sakemaru_handy_denso.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Main : Routes("main")
    object WarehouseSettings : Routes("warehouse_settings")
    object Inbound : Routes("inbound")

    // Outbound routes (2.5.1 - 2.5.4 spec flow)
    object OutboundSelect : Routes("outbound_select")
    object PickingList : Routes("picking_list")
    object OutboundEntry : Routes("outbound_entry/{courseId}") {
        fun createRoute(courseId: String) = "outbound_entry/$courseId"
    }
    object OutboundHistory : Routes("outbound_history?courseId={courseId}") {
        fun createRoute(courseId: String? = null) = if (courseId != null) {
            "outbound_history?courseId=$courseId"
        } else {
            "outbound_history"
        }
    }
    object SlipEntry : Routes("slip_entry")

    // Legacy outbound routes (to be migrated/removed)
    object OutboundList : Routes("outbound_list")
    object OutboundDetail : Routes("outbound_detail/{orderId}") {
        fun createRoute(orderId: String) = "outbound_detail/$orderId"
    }
    object OutboundConfirm : Routes("outbound_confirm/{orderId}") {
        fun createRoute(orderId: String) = "outbound_confirm/$orderId"
    }

    object Move : Routes("move")
    object Inventory : Routes("inventory")
    object LocationSearch : Routes("location_search")
}
