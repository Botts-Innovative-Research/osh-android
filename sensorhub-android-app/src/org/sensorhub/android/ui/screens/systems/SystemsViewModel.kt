package org.sensorhub.android.ui.screens.systems

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class SystemsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NodeRepository.getInstance(application)
    val savedServerOptions = repository.savedServerOptions
    val selectedServerStates = repository.selectedServerStates
    val selectedIds = repository.selectedIds

    fun open() = repository.open()
    fun setSelected(id: String, selected: Boolean) = repository.setSelected(id, selected)

    fun refresh(serverId: String) = repository.refresh(serverId)
}
