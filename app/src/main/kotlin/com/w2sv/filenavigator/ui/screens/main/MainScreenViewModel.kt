package com.w2sv.filenavigator.ui.screens.main

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.w2sv.androidutils.coroutines.getSynchronousMap
import com.w2sv.androidutils.coroutines.getValueSynchronously
import com.w2sv.androidutils.coroutines.mapState
import com.w2sv.androidutils.eventhandling.BackPressHandler
import com.w2sv.androidutils.notifying.showToast
import com.w2sv.androidutils.services.isServiceRunning
import com.w2sv.androidutils.ui.PreferencesDataStoreBackedUnconfirmedStatesViewModel
import com.w2sv.androidutils.ui.UnconfirmedStateFlow
import com.w2sv.androidutils.ui.UnconfirmedStatesComposition
import com.w2sv.filenavigator.FileType
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.datastore.PreferencesDataStoreRepository
import com.w2sv.filenavigator.datastore.PreferencesKey
import com.w2sv.filenavigator.navigator.service.FileNavigatorService
import com.w2sv.filenavigator.utils.StorageAccessStatus
import com.w2sv.filenavigator.utils.getMutableStateMap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import slimber.log.i
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    dataStoreRepository: PreferencesDataStoreRepository
) : PreferencesDataStoreBackedUnconfirmedStatesViewModel<PreferencesDataStoreRepository>(
    dataStoreRepository
) {

    val isNavigatorRunning: MutableStateFlow<Boolean> =
        MutableStateFlow(context.isServiceRunning<FileNavigatorService>())

    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    val disableListenerOnLowBattery = makeUnconfirmedStateFlow(
        dataStoreRepository.disableListenerOnLowBattery,
        PreferencesKey.DISABLE_LISTENER_ON_LOW_BATTERY
    )

    val inAppTheme = makeUnconfirmedEnumValuedStateFlow(
        dataStoreRepository.inAppTheme,
        PreferencesKey.IN_APP_THEME
    )

    val unconfirmedExtendedSettings =
        makeUnconfirmedStatesComposition(listOf(disableListenerOnLowBattery, inAppTheme))

    // ==============
    // StorageAccessStatus
    // ==============

    val storageAccessStatus: StateFlow<StorageAccessStatus> get() = _storageAccessStatus
    private val _storageAccessStatus = MutableStateFlow(StorageAccessStatus.NoAccess)

    val anyStorageAccessGranted: StateFlow<Boolean> =
        storageAccessStatus.mapState { it != StorageAccessStatus.NoAccess }

    fun updateStorageAccessStatus(context: Context) {
        _storageAccessStatus.value = StorageAccessStatus.get(context)
            .also { status ->
                val previousStatus =
                    dataStoreRepository.previousStorageAccessStatus.getValueSynchronously()

                if (status != previousStatus) {
                    i { "New manageExternalStoragePermissionGranted = $status diverting from previous = $previousStatus" }

                    when (status) {
                        StorageAccessStatus.NoAccess -> setFileTypeStatuses(
                            FileType.all,
                            FileType.Status.DisabledForNoFileAccess
                        )

                        StorageAccessStatus.MediaFilesOnly -> {
                            setFileTypeStatuses(
                                FileType.NonMedia.all,
                                FileType.Status.DisabledForMediaAccessOnly
                            )

                            if (previousStatus == StorageAccessStatus.NoAccess) {
                                setFileTypeStatuses(FileType.Media.all, FileType.Status.Enabled)
                            }
                        }

                        StorageAccessStatus.AllFiles -> setFileTypeStatuses(
                            FileType.all,
                            FileType.Status.Enabled
                        )
                    }

                    coroutineScope.launch {
                        dataStoreRepository.save(
                            PreferencesKey.PREVIOUS_STORAGE_ACCESS_STATUS,
                            status
                        )
                    }
                }
            }
    }

    private fun setFileTypeStatuses(fileTypes: Iterable<FileType>, newStatus: FileType.Status) {
        coroutineScope.launch {
            dataStoreRepository.saveEnumValuedMap(
                fileTypes.map { it.status }.associateWith { newStatus }
            )
            fileTypes.forEach {
                unconfirmedFileTypeStatus[it.status] = newStatus
            }
        }
    }

    // ==============
    // Navigator Configuration
    // ==============

    val unconfirmedFileTypeStatus by lazy {
        makeUnconfirmedEnumValuedStateMap(
            appliedFlowMap = dataStoreRepository.fileTypeStatus,
            makeMutableMap = { it.getSynchronousMap().getMutableStateMap() }
        )
    }

    val unconfirmedFileSourceEnablement by lazy {
        makeUnconfirmedStateMap(
            appliedFlowMap = dataStoreRepository.mediaFileSourceEnabled,
            makeMutableMap = { it.getSynchronousMap().getMutableStateMap() }
        )
    }

    val unconfirmedNavigatorConfiguration by lazy {
        makeUnconfirmedStatesComposition(
            listOf(
                unconfirmedFileTypeStatus,
                unconfirmedFileSourceEnablement
            )
        )
    }

    fun setUnconfirmedDefaultMoveDestinationStates(fileSource: FileType.Source) {
        unconfirmedDefaultMoveDestination = makeUnconfirmedUriValuedStateFlow(
            dataStoreRepository.getFileSourceDefaultDestinationFlow(fileSource),
            fileSource.defaultDestination.preferencesKey
        )
        unconfirmedDefaultMoveDestinationIsLocked = makeUnconfirmedStateFlow(
            dataStoreRepository.getFileSourceDefaultDestinationIsLockedFlow(fileSource),
            fileSource.defaultDestinationIsLocked.preferencesKey
        )

        unconfirmedDefaultMoveDestinationConfiguration = makeUnconfirmedStatesComposition(
            listOf(
                unconfirmedDefaultMoveDestination!!,
                unconfirmedDefaultMoveDestinationIsLocked!!
            )
        )
    }

    fun unsetUnconfirmedDefaultMoveDestinationStates(){
        unconfirmedDefaultMoveDestination = null
        unconfirmedDefaultMoveDestinationIsLocked = null
        unconfirmedDefaultMoveDestinationConfiguration = null
    }

    var unconfirmedDefaultMoveDestination: UnconfirmedStateFlow<Uri?>? = null
    var unconfirmedDefaultMoveDestinationIsLocked: UnconfirmedStateFlow<Boolean>? = null
    var unconfirmedDefaultMoveDestinationConfiguration: UnconfirmedStatesComposition? = null

    val launchDefaultMoveDestinationPickerFor: MutableStateFlow<FileType.Source?> =
        MutableStateFlow(null)

    // ==============
    // BackPress Handling
    // ==============

    val exitApplication = MutableSharedFlow<Unit>()

    fun onBackPress(context: Context) {
        backPressHandler.invoke(
            onFirstPress = {
                context.showToast(context.getString(R.string.tap_again_to_exit))
            },
            onSecondPress = {
                viewModelScope.launch {
                    exitApplication.emit(Unit)
                }
            }
        )
    }

    private val backPressHandler = BackPressHandler(
        viewModelScope,
        2500L
    )
}