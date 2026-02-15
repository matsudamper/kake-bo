package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.runtime.Composable

@Composable
public expect fun ZoomableImageDialog(
    imageUrl: String,
    onDismissRequest: () -> Unit,
)
