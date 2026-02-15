package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.matsudamper.money.element.ImageId

@Composable
public expect fun ImageUploadButton(
    onUploaded: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
)
