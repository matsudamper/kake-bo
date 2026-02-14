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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.base.ColorUtil
import net.matsudamper.money.frontend.common.base.HsvColor
import net.matsudamper.money.frontend.common.ui.layout.TextField

private val presetColors: List<Color> = listOf(
    Color(0xFFF44336),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF673AB7),
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF03A9F4),
    Color(0xFF00BCD4),
    Color(0xFF009688),
    Color(0xFF4CAF50),
    Color(0xFF8BC34A),
    Color(0xFFCDDC39),
    Color(0xFFFFEB3B),
    Color(0xFFFFC107),
    Color(0xFFFF9800),
    Color(0xFFFF5722),
    Color(0xFF795548),
    Color(0xFF9E9E9E),
    Color(0xFF607D8B),
    Color(0xFF000000),
)

@Composable
public fun ColorPickerDialog(
    currentColor: Color?,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    var hsvState by remember {
        val initial = if (currentColor != null) {
            HsvColor.fromColor(currentColor)
        } else {
            HsvColor(0f, 1f, 1f)
        }
        mutableStateOf(initial)
    }

    var hexInput by remember {
        mutableStateOf(hsvState.toHexString())
    }

    val currentColor = remember(hsvState) { hsvState.toColor() }
    val isHexInputValid = remember(hexInput) {
        hexInput.isEmpty() || ColorUtil.isValidHexColor(hexInput)
    }

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
                            hexInput = newHsv.toHexString()
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
                            hexInput = newHsv.toHexString()
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
                                val parsed = HsvColor.fromHex(filtered)
                                if (parsed != null) {
                                    hsvState = parsed
                                }
                            }
                        },
                        singleLine = true,
                        label = "HEX",
                        prefix = { Text("#") },
                        modifier = Modifier.weight(1f),
                        isError = isHexInputValid.not(),
                        supportingText = {
                            if (isHexInputValid.not()) {
                                Text(
                                    text = "6桁の16進数で入力してください",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
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
                        val isSelected = currentColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
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
                                    val hsv = HsvColor.fromColor(color)
                                    hsvState = hsv
                                    hexInput = hsv.toHexString()
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
                            onColorSelected(currentColor)
                        },
                        enabled = isHexInputValid,
                    ) {
                        Text("決定")
                    }
                }
            }
        }
    }
}
