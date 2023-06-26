package com.w2sv.filenavigator.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anggrayudi.storage.file.getSimplePath
import com.w2sv.androidutils.coroutines.launchDelayed
import com.w2sv.filenavigator.ui.model.FileType
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.ui.components.AppFontText
import com.w2sv.filenavigator.ui.components.DialogButton
import com.w2sv.filenavigator.ui.theme.DefaultIconDp
import com.w2sv.filenavigator.ui.theme.disabledColor
import kotlinx.coroutines.Job

@Composable
fun OpenFileSourceDefaultDestinationDialogButton(
    source: FileType.Source,
    modifier: Modifier = Modifier,
    mainScreenViewModel: MainScreenViewModel = viewModel()
) {
    var defaultDestinationDialogFileSource by rememberSaveable {
        mutableStateOf<FileType.Source?>(null)
    }
        .apply {
            value?.let {
                if (mainScreenViewModel.unconfirmedDefaultMoveDestinationConfiguration == null) {
                    mainScreenViewModel.setUnconfirmedDefaultMoveDestinationStates(source)
                }

                DefaultMoveDestinationDialog(
                    fileSource = it,
                    closeDialog = {
                        value = null
                    }
                )
            }
        }

    IconButton(
        onClick = { defaultDestinationDialogFileSource = source },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(
                id = if (mainScreenViewModel.defaultMoveDestinationIsSet.getValue(
                        source.defaultDestination
                    )
                )
                    R.drawable.ic_edit_folder_24
                else
                    R.drawable.ic_add_new_folder_24
            ),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = stringResource(
                R.string.open_target_directory_settings
            ),
            modifier = Modifier.size(DefaultIconDp)
        )
    }
}

@Composable
private fun DefaultMoveDestinationDialog(
    fileSource: FileType.Source,
    closeDialog: () -> Unit,
    modifier: Modifier = Modifier,
    mainScreenViewModel: MainScreenViewModel = viewModel()
) {
    val onDismissRequest: () -> Unit = {
        mainScreenViewModel.unsetUnconfirmedDefaultMoveDestinationStates()
        closeDialog()
    }

    val defaultMoveDestination by mainScreenViewModel.unconfirmedDefaultMoveDestination!!.collectAsState()
    val defaultMoveDestinationIsLocked by mainScreenViewModel.unconfirmedDefaultMoveDestinationIsLocked!!.collectAsState()
    val configurationHasChanged by mainScreenViewModel.unconfirmedDefaultMoveDestinationConfiguration!!.statesDissimilar.collectAsState()

    val isDestinationSet by remember {
        derivedStateOf { defaultMoveDestination != null }
    }
    var showLockInfo by rememberSaveable {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var closeLogInfoJob: Job? = null

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.padding(horizontal = 32.dp),
        onDismissRequest = { onDismissRequest() },
        dismissButton = {
            DialogButton(onClick = onDismissRequest) {
                AppFontText(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            DialogButton(
                onClick = {
                    with(mainScreenViewModel) {
                        defaultMoveDestinationIsSet[fileSource.defaultDestination] =
                            isDestinationSet
                        unconfirmedDefaultMoveDestinationConfiguration!!.launchSync()
                    }
                    onDismissRequest()
                },
                enabled = configurationHasChanged
            ) {
                AppFontText(text = stringResource(id = R.string.apply))
            }
        },
        icon = {
            Row {
                Icon(
                    painter = painterResource(id = fileSource.fileType.iconRes),
                    contentDescription = null,
                    tint = fileSource.fileType.color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = fileSource.kind.iconRes),
                    contentDescription = null,
                    tint = fileSource.fileType.color,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            AppFontText(
                text = buildAnnotatedString {
                    append("Default ")
                    withStyle(SpanStyle(color = fileSource.fileType.color)) {
                        append(fileSource.getTitle(context))
                    }
                    append(" Move Destination")
                },
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppFontText(
                        text = defaultMoveDestination?.let {
                            DocumentFile.fromSingleUri(context, it)?.getSimplePath(context)
                        } ?: stringResource(R.string.not_set),
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(0.8f),
                        color = if (isDestinationSet) Color.Unspecified else disabledColor()
                    )
                    // Pick button
                    IconButton(
                        onClick = {
                            mainScreenViewModel.launchDefaultMoveDestinationPickerFor.value =
                                fileSource
                        },
                        modifier = Modifier.weight(0.15f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open_24),
                            contentDescription = stringResource(
                                R.string.change_default_move_destination
                            ),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    // Delete button
                    IconButton(
                        onClick = {
                            mainScreenViewModel.unconfirmedDefaultMoveDestination!!.value = null
                            mainScreenViewModel.unconfirmedDefaultMoveDestinationIsLocked!!.value =
                                false
                        },
                        modifier = Modifier.weight(0.15f),
                        enabled = isDestinationSet
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_24),
                            contentDescription = stringResource(R.string.delete_set_default_move_destination),
                            tint = if (isDestinationSet) MaterialTheme.colorScheme.secondary else disabledColor()
                        )
                    }
                    // Lock button
                    IconButton(
                        onClick = {
                            mainScreenViewModel.unconfirmedDefaultMoveDestinationIsLocked!!.value =
                                !mainScreenViewModel.unconfirmedDefaultMoveDestinationIsLocked!!.value
                            showLockInfo = true
                        },
                        modifier = Modifier.weight(0.15f),
                        enabled = isDestinationSet
                    ) {
                        AnimatedVisibility(visible = defaultMoveDestinationIsLocked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock_closed_24),
                                contentDescription = stringResource(R.string.unlock_default_move_destination),
                                tint = if (isDestinationSet) MaterialTheme.colorScheme.secondary else disabledColor()
                            )
                        }
                        AnimatedVisibility(visible = !defaultMoveDestinationIsLocked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock_open_24),
                                contentDescription = stringResource(R.string.lock_default_move_destination),
                                tint = if (isDestinationSet) MaterialTheme.colorScheme.secondary else disabledColor()
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = showLockInfo) {
                    AppFontText(
                        text = if (defaultMoveDestinationIsLocked)
                            stringResource(R.string.default_move_destination_locked_info)
                        else
                            stringResource(R.string.default_move_destination_unlocked_info),
                        color = disabledColor()
                    )
                    LaunchedEffect(key1 = Unit) {
                        closeLogInfoJob?.cancel()
                        closeLogInfoJob = scope.launchDelayed(5000L) {
                            showLockInfo = false
                        }
                    }
                }
            }
        }
    )
}

//@Preview
//@Composable
//private fun DefaultMoveDestinationDialogPrev() {
//    AppTheme {
//        DefaultMoveDestinationDialog(
//            fileSource = FileType.Source(FileType.Media.Image, FileType.SourceKind.Camera),
//            defaultMoveDestination = null,
//            closeDialog = {},
//            setDefaultDestination = {},
//            resetDefaultDestination = {},
//            unconfirmedDefaultMoveDestinationState = UnconfirmedStateFlow()
//        )
//    }
//}