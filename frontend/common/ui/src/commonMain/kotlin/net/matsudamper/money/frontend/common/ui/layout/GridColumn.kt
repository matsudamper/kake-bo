package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap

@Composable
internal fun GridColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    verticalPadding: Dp = 0.dp,
    content: GridColumnScope.() -> Unit,
) {
    val latestContent by rememberUpdatedState(content)
    val columnScope by remember {
        derivedStateOf { GridColumnScopeImpl().apply(latestContent) }
    }
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxColumnCount = columnScope.rowScopes.maxOf { it.rowContents.size }
        val maxRowCount = columnScope.rowScopes.size
        val maxHeightAssociateByRowIndex = mutableMapOf<Int, Int>()

        val columns = run columns@{
            var currentWidth = 0
            (0 until maxColumnCount).map { columnIndex ->
                columnScope.rowScopes.mapIndexed { rowIndex, rowScope ->
                    maxHeightAssociateByRowIndex[rowIndex] = 0
                    val rowContent = rowScope.rowContents.getOrNull(columnIndex)
                    val rowGroup = subcompose("${columnIndex}_${rowIndex}") {
                        Column {
                            Row {
                                rowContent?.invoke()
                                if (maxColumnCount != columnIndex + 1) {
                                    Spacer(Modifier.width(horizontalPadding))
                                }
                            }
                            if (maxRowCount != rowIndex + 1) {
                                Spacer(Modifier.height(verticalPadding))
                            }
                        }
                    }

                    val rowMeasurables = rowGroup.fastMap { item ->
                        item.measure(
                            Constraints(
                                minWidth = 0,
                                minHeight = 0,
                                maxWidth = (constraints.maxWidth - currentWidth),
                                maxHeight = constraints.maxHeight
                            )
                        )
                    }

                    maxHeightAssociateByRowIndex[rowIndex] = maxHeightAssociateByRowIndex[rowIndex]!!
                        .plus(rowMeasurables.maxOfOrNull { it.height } ?: 0)
                    rowMeasurables
                }.also { column ->
                    currentWidth += column.flatten().maxOfOrNull { it.width } ?: 0
                }
            }
        }

        val yMap = (0..maxHeightAssociateByRowIndex.keys.sumOf { it }).map {
            (0 until it).sumOf { index -> maxHeightAssociateByRowIndex[index] ?: 0 }
        }
        val xMap = run {
            val columnWidth = columns.map { column ->
                column.maxOfOrNull { rowGroup ->
                    rowGroup.maxOfOrNull { it.width } ?: 0
                } ?: 0
            }

            (0..columnWidth.size).map {
                (0 until it).sumOf { index -> columnWidth[index] }
            }
        }

        layout(
            width = columns.sumOf { column ->
                column.maxOfOrNull { rowGroup ->
                    rowGroup.maxOfOrNull { it.width } ?: 0
                } ?: 0
            }.coerceAtLeast(constraints.minWidth),
            height = run {
                columns.maxOfOrNull { column ->
                    column.sumOf { rowGroup ->
                        rowGroup.maxOfOrNull { it.height } ?: 0
                    } ?: 0
                } ?: 0
            }.coerceAtLeast(constraints.minHeight),
        ) {
            for (columnIndex in columns.indices) {
                val column = columns[columnIndex]
                for (rowIndex in column.indices) {
                    val rowGroup = column[rowIndex]
                    rowGroup.forEach { item ->
                        item.place(x = xMap[columnIndex], y = yMap[rowIndex])
                    }
                }
            }
        }
    }
}

private class GridColumnScopeImpl : GridColumnScope {
    val rowScopes = mutableListOf<RowScopeImpl>()
    override fun row(content: GridColumnScope.RowScope.() -> Unit) {
        val scope = RowScopeImpl().apply(content)
        rowScopes.add(scope)
    }

    class RowScopeImpl : GridColumnScope.RowScope {
        val rowContents: MutableList<@Composable () -> Unit> = mutableListOf()
        override fun item(content: @Composable () -> Unit) {
            rowContents.add(content)
        }
    }
}

internal interface GridColumnScope {
    fun row(content: RowScope.() -> Unit)
    interface RowScope {
        fun item(content: @Composable () -> Unit)
    }
}
