package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlertDialog(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    positiveButton: (@Composable () -> Unit)? = null,
    negativeButton: (@Composable () -> Unit)? = null,
    onClickPositive: () -> Unit,
    onClickNegative: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismissRequest() }
            .zIndex(Float.MAX_VALUE)
            .background(Color.Black.copy(0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* Nothing */ }
                .zIndex(Float.MAX_VALUE)
                .widthIn(max = 700.dp)
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp, top = 24.dp)
                    .width(IntrinsicSize.Max),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        title()
                    }
                }

                if (description != null) {
                    Spacer(Modifier.height(8.dp))
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            description()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onClickNegative() }) {
                        if (negativeButton != null) {
                            negativeButton()
                        } else {
                            Text("CANCEL")
                        }
                    }
                    TextButton(onClick = { onClickPositive() }) {
                        if (positiveButton != null) {
                            positiveButton()
                        } else {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
