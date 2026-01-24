package biz.smt_life.android.sakemaru_handy_denso.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Main : Routes("main")
    object Settings : Routes("settings")
    object WarehouseSettings : Routes("warehouse_settings")
    object Inbound : Routes("inbound")
    object InboundWebView : Routes("inbound_webview")

    // Outbound routes (2.5.1 - 2.5.4 spec flow)
    object PickingList : Routes("picking_list") // 2.5.1 - コース選択
    object OutboundPicking : Routes("outbound_picking/{taskId}") { // 2.5.2 - データ入力
        fun createRoute(taskId: Int) = "outbound_picking/$taskId"
    }
    object PickingHistory : Routes("picking_history/{taskId}") { // 2.5.3 - 履歴
        fun createRoute(taskId: Int) = "picking_history/$taskId"
    }

    object SlipEntry : Routes("slip_entry")

    object Move : Routes("move")
    object Inventory : Routes("inventory")
    object LocationSearch : Routes("location_search")
}
