package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun UrlMenuDialog(
    url: String,
    onDismissRequest: () -> Unit,
    onClickOpen: () -> Unit,
    onClickCopy: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        content = {
            Card(
                modifier = Modifier
                    .width(intrinsicSize = IntrinsicSize.Min)
                    .widthIn(min = 500.dp, max = 700.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                ) {
                    Text(
                        text = url,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = rememberCustomFontFamily(),
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(modifier = Modifier.fillMaxWidth())
                    val modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    val style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = rememberCustomFontFamily(),
                    )
                    Text(
                        modifier = Modifier
                            .clickable { onClickOpen() }
                            .then(modifier),
                        text = "開く",
                        style = style,
                    )
                    Text(
                        modifier = Modifier
                            .clickable { onClickCopy() }
                            .then(modifier),
                        text = "コピー",
                        style = style,
                    )
                }
            }
        },
    )
}