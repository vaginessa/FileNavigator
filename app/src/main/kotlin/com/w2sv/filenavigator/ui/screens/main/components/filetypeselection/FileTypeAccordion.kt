package com.w2sv.filenavigator.ui.screens.main.components.filetypeselection

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getSimplePath
import com.w2sv.common.utils.goToManageExternalStorageSettings
import com.w2sv.common.utils.manageExternalStoragePermissionRequired
import com.w2sv.data.model.FileType
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.ui.components.AppCheckbox
import com.w2sv.filenavigator.ui.components.AppFontText
import com.w2sv.filenavigator.ui.components.AppSnackbarVisuals
import com.w2sv.filenavigator.ui.components.LocalSnackbarHostState
import com.w2sv.filenavigator.ui.components.SnackbarAction
import com.w2sv.filenavigator.ui.components.SnackbarKind
import com.w2sv.filenavigator.ui.components.showSnackbarAndDismissCurrent
import com.w2sv.filenavigator.ui.model.color
import com.w2sv.filenavigator.ui.model.toggle
import com.w2sv.filenavigator.ui.states.NavigatorUIState
import com.w2sv.filenavigator.ui.theme.AppColor
import com.w2sv.filenavigator.ui.theme.DefaultAnimationDuration
import com.w2sv.filenavigator.ui.theme.DefaultIconDp
import com.w2sv.filenavigator.ui.theme.Epsilon
import com.w2sv.filenavigator.ui.utils.CascadeAnimationState
import com.w2sv.filenavigator.ui.utils.InBetweenSpaced
import com.w2sv.filenavigator.ui.utils.extensions.allFalseAfterEnteringValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun FileTypeAccordion(
    fileType: FileType,
    isFirstDisabled: Boolean,
    navigatorUIState: NavigatorUIState,
    cascadeAnimationState: CascadeAnimationState<FileType>,
    modifier: Modifier = Modifier
) {
    val animationImpending = cascadeAnimationState.animationImpending(fileType)
    var animatedProgress by remember { mutableFloatStateOf(if (animationImpending) 0f else 1f) }

    if (animationImpending) {
        LaunchedEffect(key1 = fileType) {
            cascadeAnimationState.onAnimationStarted(fileType)
            animatedProgress = 1f
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(
            durationMillis = DefaultAnimationDuration,
            delayMillis = cascadeAnimationState.animationDelayMillis
        ),
        label = "",
        finishedListener = {
            cascadeAnimationState.onAnimationFinished()
        }
    )

    Column(
        modifier = modifier.graphicsLayer(
            alpha = animatedAlpha,
            scaleX = animatedAlpha,
            scaleY = animatedAlpha
        )
    ) {
        if (isFirstDisabled) {
            DisabledText(modifier = Modifier.padding(bottom = 8.dp))
        }

        val fileTypeEnabled by remember {
            derivedStateOf { navigatorUIState.fileTypeStatusMap.getValue(fileType.status).isEnabled }  // TODO: derivedState necessary?
        }
        FileTypeAccordionHeader(
            fileType = fileType,
            isEnabled = fileTypeEnabled,
            fileTypeStatusMap = navigatorUIState.fileTypeStatusMap
        )
        AnimatedVisibility(
            visible = fileTypeEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FileTypeSourcesSurface(
                fileType = fileType,
                navigatorUIState = navigatorUIState
            )
        }
    }
}

@Composable
private fun DisabledText(modifier: Modifier = Modifier) {
    AppFontText(
        text = stringResource(R.string.disabled),
        fontSize = 16.sp,
        color = AppColor.disabled,
        modifier = modifier
    )
}

@Composable
private fun FileTypeAccordionHeader(
    fileType: FileType,
    isEnabled: Boolean,
    fileTypeStatusMap: MutableMap<FileType.Status.StoreEntry, FileType.Status>,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    snackbarHostState: SnackbarHostState = LocalSnackbarHostState.current,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = fileType.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = if (isEnabled) fileType.color else AppColor.disabled
                )
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.CenterStart) {
                AppFontText(
                    text = stringResource(id = fileType.titleRes),
                    fontSize = 18.sp,
                    color = if (isEnabled) Color.Unspecified else AppColor.disabled
                )
            }
            Box(
                modifier = Modifier
                    .weight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                Switch(
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(8.dp),
                    checked = isEnabled,
                    onCheckedChange = { checkedNew ->
                        when (val status =
                            fileTypeStatusMap.getValue(fileType.status)) {
                            FileType.Status.Enabled, FileType.Status.Disabled -> {
                                if (!fileTypeStatusMap.values.map { it.isEnabled }
                                        .allFalseAfterEnteringValue(
                                            checkedNew
                                        )
                                ) {
                                    fileTypeStatusMap.toggle(fileType.status)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbarAndDismissCurrent(
                                            AppSnackbarVisuals(
                                                message = context.getString(
                                                    R.string.leave_at_least_one_file_type_enabled
                                                ),
                                                kind = SnackbarKind.Error
                                            )
                                        )
                                    }
                                }
                            }

                            FileType.Status.DisabledDueToNoFileAccess, FileType.Status.DisabledDueToMediaAccessOnly -> {
                                scope.launch {
                                    snackbarHostState.showSnackbarAndDismissCurrent(
                                        getManageExternalStorageSnackbarVisuals(status, context)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Assumes [fileTypeStatus] to be one of [FileType.Status.DisabledDueToNoFileAccess], [FileType.Status.DisabledDueToMediaAccessOnly].
 */
private fun getManageExternalStorageSnackbarVisuals(
    fileTypeStatus: FileType.Status,
    context: Context
): SnackbarVisuals =
    AppSnackbarVisuals(
        message = context.getString(
            if (fileTypeStatus == FileType.Status.DisabledDueToNoFileAccess)
                R.string.manage_external_storage_permission_rational
            else
                R.string.non_media_files_require_all_files_access
        ),
        kind = SnackbarKind.Error,
        action = SnackbarAction(
            label = context.getString(R.string.grant),
            callback = {
                if (manageExternalStoragePermissionRequired()) {
                    goToManageExternalStorageSettings(context)
                }
            }
        )
    )

@Composable
private fun FileTypeSourcesSurface(
    fileType: FileType,
    navigatorUIState: NavigatorUIState,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            InBetweenSpaced(
                elements = fileType.sources,
                makeElement = {
                    FileTypeSourceConfigurationView(
                        source = it,
                        navigatorUIState = navigatorUIState
                    )
                }
            )
        }
    }
}

@Composable
private fun FileTypeSourceConfigurationView(
    source: FileType.Source,
    navigatorUIState: NavigatorUIState,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val isEnabled =
        if (source.fileType.isMediaType) navigatorUIState.mediaFileSourceEnabledMap.getValue(
            source.isEnabled
        ) else true
    val defaultDestination by navigatorUIState.defaultDestinationStateFlowMap.getValue(source.defaultDestination)
        .collectAsState()
    val defaultDestinationPath by remember {
        derivedStateOf { defaultDestination?.let { getDefaultMoveDestinationPath(it, context) } }
    }

    Column(modifier = modifier) {
        FileSourceRow(
            isEnabled = isEnabled,
            source = source,
            navigatorUIState = navigatorUIState,
            modifier = Modifier.height(44.dp)
        )

        AnimatedVisibility(visible = isEnabled && defaultDestinationPath != null) {
            DefaultMoveDestinationRow(
                path = remember(this) {  // Remedies NullPointerException
                    defaultDestinationPath!!
                },
                onDeleteButtonClick = {
                    navigatorUIState.saveDefaultDestination(source, null)
                },
                modifier = Modifier
                    .height(36.dp)
                    .padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun FileSourceRow(
    isEnabled: Boolean,
    source: FileType.Source,
    navigatorUIState: NavigatorUIState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = LocalSnackbarHostState.current,
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        // Source icon
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = source.kind.iconRes),
                contentDescription = null,
                tint = if (isEnabled) source.fileType.color.copy(alpha = 0.75f) else AppColor.disabled
            )
        }
        // Source label
        Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.CenterStart) {
            AppFontText(
                text = stringResource(id = source.kind.labelRes),
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface.copy(0.7f) else AppColor.disabled
            )
        }

        val buttonBoxWeight = 0.1f
        val destinationButtonBoxWeight by animateFloatAsState(
            targetValue = if (isEnabled) buttonBoxWeight else Epsilon,
            label = ""
        )

        // Empty box, pushing the checkbox into the position of the destinationButtonBox upon vanishing of the latter
        Spacer(modifier = Modifier.weight(buttonBoxWeight - destinationButtonBoxWeight + Epsilon))
        // CheckboxContent
        Box(modifier = Modifier.weight(buttonBoxWeight), contentAlignment = Alignment.Center) {
            if (source.fileType.isMediaType) {
                AppCheckbox(
                    checked = isEnabled,
                    onCheckedChange = { checkedNew ->
                        if (!source.fileType.sources.map {
                                navigatorUIState.mediaFileSourceEnabledMap.getValue(
                                    it.isEnabled
                                )
                            }
                                .allFalseAfterEnteringValue(checkedNew)
                        ) {
                            navigatorUIState.mediaFileSourceEnabledMap[source.isEnabled] =
                                checkedNew
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbarAndDismissCurrent(
                                    AppSnackbarVisuals(
                                        message = context.getString(R.string.leave_at_least_one_file_source_selected_or_disable_the_entire_file_type),
                                        kind = SnackbarKind.Error
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }

        // Destination Button
        Box(
            modifier = Modifier
                .weight(destinationButtonBoxWeight)
                .alpha(destinationButtonBoxWeight * 10),
            contentAlignment = Alignment.Center
        ) {
            SetDefaultMoveDestinationButton(
                onClick = { navigatorUIState.setDefaultMoveDestinationSource.value = source }
            )
        }
    }
}

@Composable
private fun SetDefaultMoveDestinationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(
                id = com.w2sv.navigator.R.drawable.ic_file_move_24
            ),
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = stringResource(
                R.string.open_target_directory_settings
            ),
            modifier = Modifier.size(DefaultIconDp)
        )
    }
}

fun getDefaultMoveDestinationPath(uri: Uri, context: Context): String? =
    DocumentFile.fromSingleUri(context, uri)?.getSimplePath(context)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultMoveDestinationRow(
    path: String,
    onDeleteButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.fillMaxWidth(0.22f))
        AppFontText(
            text = path,
            color = AppColor.disabled,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.7f)
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            IconButton(
                onClick = onDeleteButtonClick,
                modifier = Modifier.weight(0.1f)
            ) {
                Icon(
                    painter = painterResource(id = com.w2sv.navigator.R.drawable.ic_delete_24),
                    contentDescription = stringResource(R.string.delete_default_move_destination),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
