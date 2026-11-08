package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminThumbnailBackfillScreen(
    modifier: Modifier = Modifier,
    uiState: AdminThumbnailBackfillScreenUiState,
    onClickBack: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = "戻る")
                    }
                },
                title = {
                    Text(
                        text = "サムネイル生成",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                actions = {
                    if (uiState.isRunning) {
                        TextButton(onClick = { uiState.event.onClickStop() }) {
                            Text(
                                text = "停止",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    } else {
                        TextButton(onClick = { uiState.event.onClickStart() }) {
                            Text(
                                text = "開始",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        ConsoleLog(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            lines = uiState.lines,
        )
    }
}

@Composable
private fun ConsoleLog(
    modifier: Modifier = Modifier,
    lines: List<AdminThumbnailBackfillScreenUiState.Line>,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }
    LazyColumn(
        modifier = modifier
            .background(ConsoleBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        state = listState,
    ) {
        itemsIndexed(lines) { index, line ->
            val color = when (line.level) {
                AdminThumbnailBackfillScreenUiState.Level.INFO -> ConsoleInfo
                AdminThumbnailBackfillScreenUiState.Level.SUCCESS -> ConsoleSuccess
                AdminThumbnailBackfillScreenUiState.Level.ERROR -> ConsoleError
            }
            Text(
                text = "[${index + 1}] ${line.text}",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

private val ConsoleBackground = Color(0xFF1E1E1E)
private val ConsoleInfo = Color(0xFFD4D4D4)
private val ConsoleSuccess = Color(0xFF9CDCFE)
private val ConsoleError = Color(0xFFF48771)
