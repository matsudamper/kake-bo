package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton

@Composable
internal fun RootHomePeriodSection(
    modifier: Modifier = Modifier,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    betweenText: @Composable () -> Unit,
    rangeText: @Composable () -> Unit,
    onClickRange: (range: Int) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.clip(CircleShape)
                    .clickable { onClickPreviousMonth() }
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前の月")
            }
            Box(
                modifier = Modifier.clip(CircleShape)
                    .clickable { onClickNextMonth() }
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "次の月")
            }
        }
        betweenText()
        Spacer(Modifier.weight(1f))
        Box {
            var expanded by remember { mutableStateOf(false) }
            DropDownMenuButton(
                onClick = { expanded = !expanded },
            ) {
                rangeText()
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(3)
                    },
                    text = {
                        Text("3ヶ月")
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(6)
                    },
                    text = {
                        Text("6ヶ月")
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(12)
                    },
                    text = {
                        Text("12ヶ月")
                    },
                )
            }
        }
    }
}
