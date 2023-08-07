package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class ImportedMailFilterCategoryScreenUiState(
    val loadingState: LoadingState,
    val textInput: TextInput? = null,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public object Error : LoadingState
        public data class Loaded(
            val title: String,
            val category: Category?,
            val conditions: ImmutableList<Condition>,
            val operator: Operator,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public enum class Operator {
        AND,
        OR,
        UNKNOWN,
        ;

        internal fun getDisplayText(): String {
            return when (this) {
                AND -> "AND"
                OR -> "OR"
                UNKNOWN -> ""
            }
        }
    }

    public enum class DataSource {
        MailFrom,
        MailTitle,
        MailBody,
        Title,
        ServiceName,
        Unknown,
        ;

        internal fun getDisplayText(): String {
            return when (this) {
                MailFrom -> "メールアドレス"
                MailTitle -> "メールタイトル"
                MailBody -> "メール本文"
                Title -> "タイトル"
                ServiceName -> "サービス名"
                Unknown -> ""
            }
        }
    }

    public enum class ConditionType {
        Include,
        NotInclude,
        Equal,
        NotEqual,
        ;

        internal fun getDisplayText(): String {
            return when (this) {
                Include -> "含む"
                NotInclude -> "含まない"
                Equal -> "一致する"
                NotEqual -> "一致しない"
            }
        }
    }

    public data class Condition(
        val text: String,
        val source: DataSource,
        val conditionType: ConditionType,
        val event: ConditionEvent,
    )

    public data class Category(
        val category: String,
        val subCategory: String,
    )

    public data class TextInput(
        val title: String,
        val onCompleted: (String) -> Unit,
        val default: String,
        val dismiss: () -> Unit,
    )

    @Immutable
    public interface ConditionEvent {
        public fun onClickTextChange()
        public fun selectedSource(source: DataSource)
        public fun selectedConditionType(type: ConditionType)
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickAddCondition()
        public fun onClickNameChange()
        public fun onSelectedOperator(operator: Operator)
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
    }
}

@Composable
public fun ImportedMailFilterCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailFilterCategoryScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    uiState.textInput?.also { textInput ->
        HtmlFullScreenTextInput(
            title = textInput.title,
            onComplete = { textInput.onCompleted(it) },
            canceled = { textInput.dismiss() },
            default = textInput.default,
            isMultiline = false,
        )
    }
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("メールカテゴリフィルタ")
            },
        ) {
            when (val state = uiState.loadingState) {
                is ImportedMailFilterCategoryScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        onClickRetry = { uiState.event.onViewInitialized() },
                    )
                }

                is ImportedMailFilterCategoryScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                is ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = state,
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded,
) {
    LazyColumn(modifier = modifier) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = uiState.title,
                )
                OutlinedButton(
                    modifier = Modifier.padding(8.dp),
                    onClick = { uiState.event.onClickNameChange() },
                ) {
                    Text("変更")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = "次のカテゴリを適用する",
                )
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = buildString {
                            if (uiState.category != null) {
                                append("${uiState.category.category} / ${uiState.category.subCategory}")
                            } else {
                                append("未設定")
                            }
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        modifier = Modifier.padding(8.dp),
                        onClick = { uiState.event.onClickAddCondition() },
                    ) {
                        Text("変更")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = "条件",
                )
                DropDownButton(
                    modifier = Modifier.padding(8.dp),
                    item = {
                        Text(uiState.operator.name)
                    },
                    dropDown = {
                        Column {
                            immutableListOf(
                                ImportedMailFilterCategoryScreenUiState.Operator.AND,
                                ImportedMailFilterCategoryScreenUiState.Operator.OR,
                            ).forEach { operator ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { uiState.event.onSelectedOperator(operator) },
                                ) {
                                    Text(operator.getDisplayText())
                                }
                            }
                        }
                    },
                    contentDescription = "演算子を選択",
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier.padding(8.dp),
                    onClick = { uiState.event.onClickAddCondition() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("追加")
                }
            }
            Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
        }
        if (uiState.conditions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(200.dp),
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "条件がありません",
                    )
                }
            }
        } else {
            items(uiState.conditions) { item ->
                ConditionCard(
                    modifier = Modifier.fillMaxWidth(),
                    item = item,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionCard(
    modifier: Modifier = Modifier,
    item: ImportedMailFilterCategoryScreenUiState.Condition,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            FlowRow {
                DropDownButton(
                    item = {
                        Text(item.source.getDisplayText())
                    },
                    dropDown = {
                        Column {
                            immutableListOf(
                                ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom,
                                ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle,
                                ImportedMailFilterCategoryScreenUiState.DataSource.MailBody,
                                ImportedMailFilterCategoryScreenUiState.DataSource.Title,
                                ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName,
                            ).forEach { source ->
                                TextButton(
                                    modifier = Modifier.padding(8.dp),
                                    onClick = {
                                        item.event.selectedSource(source)
                                    },
                                ) {
                                    Text(source.getDisplayText())
                                }
                            }
                        }
                    },
                    contentDescription = null,
                )
                Text("が")
                DropDownButton(
                    item = {
                        Text(item.conditionType.getDisplayText())
                    },
                    dropDown = {
                        Column {
                            immutableListOf(
                                ImportedMailFilterCategoryScreenUiState.ConditionType.Include,
                                ImportedMailFilterCategoryScreenUiState.ConditionType.NotInclude,
                                ImportedMailFilterCategoryScreenUiState.ConditionType.Equal,
                                ImportedMailFilterCategoryScreenUiState.ConditionType.NotEqual,
                            ).forEach { type ->
                                TextButton(
                                    modifier = Modifier.padding(8.dp),
                                    onClick = {
                                        item.event.selectedConditionType(type)
                                    },
                                ) {
                                    Text(type.getDisplayText())
                                }
                            }
                        }
                    },
                    contentDescription = null,
                )
                Text("のとき")
            }
            Row {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.text,
                )
                OutlinedButton(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        item.text
                    },
                ) {
                    Text("変更")
                }
            }
        }
    }
}

@Composable
private fun DropDownButton(
    modifier: Modifier = Modifier,
    item: @Composable () -> Unit,
    dropDown: @Composable () -> Unit,
    contentDescription: String?,
) {
    var visibleDropDown by remember { mutableStateOf(false) }
    OutlinedButton(
        modifier = modifier,
        onClick = {
            visibleDropDown = true
        },
    ) {
        item()
        Icon(Icons.Default.ArrowDropDown, contentDescription)
    }
    DropdownMenu(
        expanded = visibleDropDown,
        onDismissRequest = {
            visibleDropDown = false
        },
        focusable = true,
    ) {
        dropDown()
    }
}
