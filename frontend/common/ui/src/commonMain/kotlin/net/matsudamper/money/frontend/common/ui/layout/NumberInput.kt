package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
public fun NumberInput(
    modifier: Modifier = Modifier,
    value: Int,
    dismissRequest: () -> Unit,
    onChangeValue: (Int) -> Unit,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.End)
                    .padding(
                        top = 4.dp,
                        end = 4.dp,
                    ),
                onClick = { dismissRequest() },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = value.toString(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
                val padColumnHeightModifier = Modifier
                    .background(Color.Red)
                    .aspectRatio(6f / 1)
                Row(
                    modifier = padColumnHeightModifier,
                ) {
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "7",
                        onClick = {
                            onChangeValue((value * 10) + 7)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "8",
                        onClick = {
                            onChangeValue((value * 10) + 8)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "9",
                        onClick = {
                            onChangeValue((value * 10) + 9)
                        },
                    )
                }
                Row(
                    modifier = padColumnHeightModifier,
                ) {
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "4",
                        onClick = {
                            onChangeValue((value * 10) + 4)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "5",
                        onClick = {
                            onChangeValue((value * 10) + 5)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "6",
                        onClick = {
                            onChangeValue((value * 10) + 6)
                        },
                    )
                }
                Row(
                    modifier = padColumnHeightModifier,
                ) {
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "1",
                        onClick = {
                            onChangeValue((value * 10) + 1)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "2",
                        onClick = {
                            onChangeValue((value * 10) + 2)
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "3",
                        onClick = {
                            onChangeValue((value * 10) + 3)
                        },
                    )
                }
                Row(
                    modifier = padColumnHeightModifier,
                ) {
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "0",
                        onClick = {
                            onChangeValue((value * 10))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "00",
                        onClick = {
                            onChangeValue((value * 100))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "DEL",
                        onClick = {
                            onChangeValue((value / 10))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPad(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.secondary)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            textAlign = TextAlign.Center,
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
