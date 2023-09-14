package com.w2sv.filenavigator.ui.screens.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w2sv.androidutils.services.isServiceRunning
import com.w2sv.data.model.StorageAccessStatus
import com.w2sv.data.storage.repositories.FileTypeRepository
import com.w2sv.data.storage.repositories.PreferencesRepository
import com.w2sv.filenavigator.ui.screens.main.states.NavigatorUIState
import com.w2sv.filenavigator.ui.screens.main.states.StorageAccessState
import com.w2sv.navigator.FileNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    fileTypeRepository: FileTypeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val isNavigatorRunning: MutableStateFlow<Boolean> =
        MutableStateFlow(context.isServiceRunning<FileNavigator>())

    val disableListenerOnLowBattery = preferencesRepository.disableListenerOnLowBattery.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        true
    )

    fun saveDisableNavigatorOnLowBattery(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveDisableNavigatorOnLowBattery(value)
        }
    }

    fun saveShowedManageExternalStorageRational(): Job =
        viewModelScope.launch {
            preferencesRepository.saveShowedManageExternalStorageRational()
        }

    val showedPostNotificationsPermissionsRational by preferencesRepository::showedPostNotificationsPermissionsRational

    fun saveShowedPostNotificationsPermissionsRational(): Job =
        viewModelScope.launch { preferencesRepository.saveShowedPostNotificationsPermissionsRational() }

    val navigatorUIState = NavigatorUIState(
        viewModelScope,
        fileTypeRepository
    )

    val storageAccessState = StorageAccessState(
        priorStatus = preferencesRepository.priorStorageAccessStatus.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            StorageAccessStatus.NoAccess
        ),
        setFileTypeStatuses = navigatorUIState::setFileTypeStatus,
        saveStorageAccessStatus = {
            viewModelScope.launch {
                preferencesRepository.savePriorStorageAccessStatus(it)
            }
        }
    )

    val showManageExternalStorageDialog = combine(
        preferencesRepository.showedManageExternalStorageRational,
        storageAccessState.anyAccessGranted,
    ) { f1, f2 ->
        println("showedManageExternalStorageRational: $f1 anyStorageAccessGranted: $f2")
        !f1 && !f2
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}