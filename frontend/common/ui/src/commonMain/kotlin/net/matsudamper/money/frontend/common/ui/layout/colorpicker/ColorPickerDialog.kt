package net.matsudamper.money.frontend.common.ui.layout.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.layout.TextField

private val presetColors: List<String> = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
    "#795548", "#9E9E9E", "#607D8B", "#000000",
)

@Composable
public fun ColorPickerDialog(
    currentColor: String?,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    var hsvState by remember {
        val initial = if (currentColor != null) {
            hsvFromHex(currentColor) ?: HsvColor(0f, 1f, 1f)
        } else {
            HsvColor(0f, 1f, 1f)
        }
        mutableStateOf(initial)
    }

    var hexInput by remember {
        mutableStateOf(currentColor?.removePrefix("#").orEmpty())
    }

    val hexString = remember(hsvState) { hsvState.toHexString() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "カテゴリーの色を選択",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    HueWheel(
                        hue = hsvState.hue,
                        onHueChanged = { newHue ->
                            val newHsv = hsvState.copy(hue = newHue)
                            hsvState = newHsv
                            hexInput = newHsv.toHexString().removePrefix("#")
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    SaturationValuePanel(
                        hue = hsvState.hue,
                        saturation = hsvState.saturation,
                        value = hsvState.value,
                        onSaturationValueChanged = { s, v ->
                            val newHsv = hsvState.copy(saturation = s, value = v)
                            hsvState = newHsv
                            hexInput = newHsv.toHexString().removePrefix("#")
                        },
                        modifier = Modifier.size(120.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(hsvState.toColor())
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        text = hexInput,
                        onValueChange = { newText ->
                            val filtered = newText
                                .filter { it.isLetterOrDigit() }
                                .take(6)
                                .uppercase()
                            hexInput = filtered
                            if (filtered.length == 6) {
                                val parsed = hsvFromHex(filtered)
                                if (parsed != null) {
                                    hsvState = parsed
                                }
                            }
                        },
                        singleLine = true,
                        label = "HEX",
                        prefix = { Text("#") },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp),
                ) {
                    items(presetColors) { color ->
                        val isSelected = hexString.equals(color, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    val hsv = hsvFromHex(color)
                                    if (hsv != null) {
                                        hsvState = hsv
                                        hexInput = color.removePrefix("#")
                                    }
                                },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onColorSelected(hexString)
                        },
                    ) {
                        Text("決定")
                    }
                }
            }
        }
    }
}
