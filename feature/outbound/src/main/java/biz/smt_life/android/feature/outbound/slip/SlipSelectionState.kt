package biz.smt_life.android.feature.outbound.slip

import biz.smt_life.android.core.domain.model.OutboundSlip

/** 伝票無し選択時に使用する特殊ID */
const val NO_SLIP_ID = "NO_SLIP"

data class SlipSelectionState(
    val isLoading: Boolean = true,
    val slips: List<OutboundSlip> = emptyList(),
    val selectedSlipId: String? = null,
    val errorMessage: String? = null
)
