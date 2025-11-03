package biz.smt_life.android.feature.settings

data class SettingsState(
    val hostUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
