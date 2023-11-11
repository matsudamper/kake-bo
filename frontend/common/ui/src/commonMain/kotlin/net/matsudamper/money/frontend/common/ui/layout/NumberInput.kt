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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.pow

public data class NumberInputValue(
    val value: Int,
    val right: Int?,
    val operator: Operator?,
) {
    public enum class Operator {
        Add,
        Minus,
        Multiply,
        Divide,
    }

    public companion object {
        public fun default(
            value: Int = 0,
        ): NumberInputValue {
            return NumberInputValue(value, null, null)
        }
    }
}

@Composable
public fun NumberInput(
    modifier: Modifier = Modifier,
    value: NumberInputValue,
    dismissRequest: () -> Unit,
    onChangeValue: (NumberInputValue) -> Unit,
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
                        text = createText(value),
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
                        text = "AC",
                        onClick = {
                            onChangeValue(NumberInputValue(0, null, null))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "8%",
                        onClick = {
                            onChangeValue(addPercent(value, 8))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "10%",
                        onClick = {
                            onChangeValue(addPercent(value, 10))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "÷",
                        onClick = {
                            onChangeValue(value.copy(operator = NumberInputValue.Operator.Divide))
                        },
                    )
                }
                Row(
                    modifier = padColumnHeightModifier,
                ) {
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "7",
                        onClick = {
                            onChangeValue(addNumberInput(value, 7))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "8",
                        onClick = {
                            onChangeValue(addNumberInput(value, 8))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "9",
                        onClick = {
                            onChangeValue(addNumberInput(value, 9))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "×",
                        onClick = {
                            onChangeValue(value.copy(operator = NumberInputValue.Operator.Multiply))
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
                            onChangeValue(addNumberInput(value, 4))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "5",
                        onClick = {
                            onChangeValue(addNumberInput(value, 5))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "6",
                        onClick = {
                            onChangeValue(addNumberInput(value, 6))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "-",
                        onClick = {
                            onChangeValue(value.copy(operator = NumberInputValue.Operator.Minus))
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
                            onChangeValue(addNumberInput(value, 1))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "2",
                        onClick = {
                            onChangeValue(addNumberInput(value, 2))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "3",
                        onClick = {
                            onChangeValue(addNumberInput(value, 3))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "+",
                        onClick = {
                            onChangeValue(value.copy(operator = NumberInputValue.Operator.Add))
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
                            onChangeValue(addDigits(value, 1))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "00",
                        onClick = {
                            onChangeValue(addDigits(value, 2))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "DEL",
                        onClick = {
                            onChangeValue(del(value))
                        },
                    )
                    NumberPad(
                        modifier = Modifier.weight(1f),
                        text = "=",
                        onClick = {
                            onChangeValue(calc(value))
                        },
                    )
                }
            }
        }
    }
}

private fun calc(value: NumberInputValue): NumberInputValue {
    if (value.right == null) {
        return value
    }
    return NumberInputValue(
        value = when (value.operator) {
            NumberInputValue.Operator.Add -> value.value + value.right
            NumberInputValue.Operator.Minus -> value.value - value.right
            NumberInputValue.Operator.Multiply -> value.value * value.right
            NumberInputValue.Operator.Divide -> value.value / value.right
            null -> value.value
        },
        right = null,
        operator = null,
    )
}
private fun roundCalcToInt(float: Float): Int {
    val floor = kotlin.math.floor((float * 10)) / 10f
    return ceil(floor).toInt()
}

private fun addPercent(value: NumberInputValue, percent: Int): NumberInputValue {
    return if (value.operator == null) {
        val result = value.value * (1 + (percent / 100f))
        value.copy(
            value = roundCalcToInt(result),
        )
    } else {
        val result = (value.right ?: 0) * (1 + (percent / 100f))
        value.copy(
            right = roundCalcToInt(result),
        )
    }
}

private fun del(value: NumberInputValue): NumberInputValue {
    return if (value.operator == null) {
        value.copy(
            value = value.value / 10,
        )
    } else {
        if (value.right == null) {
            value.copy(
                operator = null,
            )
        } else {
            value.copy(
                right = value.right / 10,
            )
        }
    }
}

private fun addDigits(value: NumberInputValue, digits: Int): NumberInputValue {
    return if (value.operator == null) {
        value.copy(
            value = (value.value * 10f.pow(digits)).toInt(),
        )
    } else {
        value.copy(
            right = ((value.right ?: 0) * 10f.pow(digits)).toInt(),
        )
    }
}

private fun addNumberInput(value: NumberInputValue, number: Int): NumberInputValue {
    return if (value.operator == null) {
        value.copy(
            value = (value.value * 10) + number,
        )
    } else {
        value.copy(
            right = (value.right ?: 0) * 10 + number,
        )
    }
}

@Stable
private fun createText(value: NumberInputValue): String {
    return buildString {
        append(value.value)
        append(
            when (value.operator) {
                NumberInputValue.Operator.Add -> " + "
                NumberInputValue.Operator.Minus -> " - "
                NumberInputValue.Operator.Multiply -> " × "
                NumberInputValue.Operator.Divide -> " ÷ "
                null -> return@buildString
            },
        )
        if (value.right != null) {
            append(value.right)
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
