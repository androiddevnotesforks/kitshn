package de.kitshn.ui.dialog.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.Uri

@Composable
fun PhotoPickerDialog(
    shown: Boolean,
    onSelect: (uri: ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    if(photoPickerDialogImpl(
        shown = shown,
        onSelect = onSelect,
        onDismiss = onDismiss
    )) return
}

@Composable
expect fun photoPickerDialogImpl(
    shown: Boolean,
    onSelect: (uri: ByteArray) -> Unit,
    onDismiss: () -> Unit
): Boolean