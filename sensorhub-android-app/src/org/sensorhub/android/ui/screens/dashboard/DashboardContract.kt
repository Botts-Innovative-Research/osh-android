package org.sensorhub.android.ui.screens.dashboard

import androidx.annotation.StringRes
import org.sensorhub.android.R
import org.sensorhub.api.module.ModuleEvent


enum class EnabledSensorCategory(@StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    ATTENTION(R.string.category_attn),
    ON_DEVICE(R.string.category_on_device),
    BLUETOOTH(R.string.category_bluetooth),
    USB(R.string.category_usb),
    OTHERS(R.string.category_others),
}
data class DashboardUiState(
    val hubStatus: ModuleEvent.ModuleState = ModuleEvent.ModuleState.LOADED,
    val sensorCards: List<SensorCardUi> = emptyList(),
    val connecting: Boolean = false,
    val serviceConnected: Boolean = false,
    val runName: String = "",
    val error: String? = null
)
