package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_upload
import org.jetbrains.compose.resources.painterResource

@Composable
public fun ImageUploadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
        border = ButtonDefaults.outlinedButtonBorder(true).copy(
            brush = SolidColor(MaterialTheme.colorScheme.primary),
        ),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_upload),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("画像をアップロード")
    }
}
