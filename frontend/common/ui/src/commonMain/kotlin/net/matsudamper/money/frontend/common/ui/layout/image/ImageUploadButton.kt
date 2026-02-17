package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

public data class SelectedImage(
    val bytes: ByteArray,
    val contentType: String?,
)

@Composable
public fun ImageUploadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text("画像をアップロード")
    }
}
