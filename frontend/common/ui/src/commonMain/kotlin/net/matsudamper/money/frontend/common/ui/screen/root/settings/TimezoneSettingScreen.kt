package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.layout.TextField
import org.jetbrains.compose.resources.painterResource

public data class TimezoneSettingScreenUiState(
    val loadingState: LoadingState,
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val event: Event,
) {
    public enum class OffsetUnit {
        Hour,
        Minute,
        ;

        public fun displayText(): String {
            return when (this) {
                Hour -> "時間"
                Minute -> "分"
            }
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data object Error : LoadingState
        public data object Loading : LoadingState
        public data class Loaded(
            val timezoneOffsetText: String,
            val inputValue: String,
            val unit: OffsetUnit,
            val event: LoadedEvent,
        ) : LoadingState
    }

    @Immutable
    public interface LoadedEvent {
        public fun onChangeValue(value: String)
        public fun onSelectUnit(unit: OffsetUnit)
        public fun onClickApply()
        public fun onClickSetDeviceTimezone()
    }

    @Immutable
    public interface Event {
        public fun onClickBack()
        public fun onResume()
        public fun onClickRetry()
    }
}

@Composable
public fun TimezoneSettingScreen(
    modifier: Modifier = Modifier,
    uiState: TimezoneSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        SettingScaffold(
            title = {
                Text("タイムゾーン設定")
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                TimezoneSettingScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = { uiState.event.onClickRetry() },
                    )
                }

                TimezoneSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TimezoneSettingScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = paddingValues,
                        uiState = loadingState,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    uiState: TimezoneSettingScreenUiState.LoadingState.Loaded,
) {
    Column(
        modifier = modifier.padding(paddingValues).padding(16.dp),
    ) {
        Text(
            text = "タイムゾーン",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "メールに時刻が含まれていない場合、メール受信時刻に適用します。",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "現在の設定: ${uiState.timezoneOffsetText}")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                modifier = Modifier.weight(1f),
                text = uiState.inputValue,
                onValueChange = { uiState.event.onChangeValue(it) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                var expanded by remember { mutableStateOf(false) }
                DropDownMenuButton(onClick = { expanded = true }) {
                    Text(uiState.unit.displayText())
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    TimezoneSettingScreenUiState.OffsetUnit.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.displayText()) },
                            onClick = {
                                uiState.event.onSelectUnit(unit)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uiState.event.onClickApply() },
        ) {
            Text("適用")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uiState.event.onClickSetDeviceTimezone() },
        ) {
            Text("端末のタイムゾーンをセット")
        }
    }
}
