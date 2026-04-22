package biz.smt_life.android.feature.settings

data class SettingsState(
    val hostUrl: String = "",
    val isCustomUrl: Boolean = false,
    val presetUrls: List<String> = PresetUrls.list,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

object PresetUrls {
    val list: List<String> = BuildConfig.PRESET_URLS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
