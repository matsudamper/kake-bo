package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.SnackbarEventState
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class ImportedMailFilterCategoryScreenUiState(
    val loadingState: LoadingState,
    val textInput: TextInput?,
    val snackbarEventState: SnackbarEventState,
    val categorySelectDialogUiState: CategorySelectDialogUiState?,
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
        MailHtml,
        MailPlain,
        Title,
        ServiceName,
        Unknown,
        ;

        internal fun getDisplayText(): String {
            return when (this) {
                MailFrom -> "メールアドレス"
                MailTitle -> "メールタイトル"
                MailHtml -> "メールHtml"
                MailPlain -> "メールテキスト"
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
        Unknown,
        ;

        internal fun getDisplayText(): String {
            return when (this) {
                Include -> "含む"
                NotInclude -> "含まない"
                Equal -> "一致する"
                NotEqual -> "一致しない"
                Unknown -> ""
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
        public fun onClickCategoryChange()
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
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    LaunchedEffect(uiState.snackbarEventState) {
        uiState.snackbarEventState.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                duration = event.duration ?: if (event.withDismissAction) {
                    SnackbarDuration.Indefinite
                } else {
                    SnackbarDuration.Short
                },
                withDismissAction = event.withDismissAction,
                actionLabel = event.actionLabel,
            )
            when (result) {
                SnackbarResult.Dismissed -> SnackbarEventState.Result.Dismiss
                SnackbarResult.ActionPerformed -> SnackbarEventState.Result.Action
            }
        }
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
        snackbarHostState = snackbarHostState,
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "メールカテゴリフィルタ",
                    )
                    IconButton(onClick = {

                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "メニューを開く",
                        )
                    }
                }
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

    uiState.categorySelectDialogUiState?.let { categorySelectDialogUiState ->
        CategorySelectDialog(
            uiState = categorySelectDialogUiState,
        )
    }
}

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
                    style = MaterialTheme.typography.titleLarge,
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
                    style = MaterialTheme.typography.titleMedium,
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
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        modifier = Modifier.padding(8.dp),
                        onClick = { uiState.event.onClickCategoryChange() },
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
                    style = MaterialTheme.typography.titleMedium,
                )
                run {
                    var visibleDropDown by remember { mutableStateOf(false) }
                    DropDownButton(
                        modifier = Modifier.padding(8.dp),
                        item = {
                            Text(uiState.operator.name)
                        },
                        visibleDropDown = visibleDropDown,
                        onDismissRequest = {
                            visibleDropDown = false
                        },
                        onClick = {
                            visibleDropDown = !visibleDropDown
                        },
                        dropDown = {
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                immutableListOf(
                                    ImportedMailFilterCategoryScreenUiState.Operator.AND,
                                    ImportedMailFilterCategoryScreenUiState.Operator.OR,
                                ).forEach { operator ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                visibleDropDown = false
                                                uiState.event.onSelectedOperator(operator)
                                            }
                                            .padding(8.dp),
                                    ) {
                                        Text(text = operator.getDisplayText())
                                    }
                                }
                            }
                        },
                        contentDescription = "演算子を選択",
                    )
                }
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
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 8.dp),
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
            FlowRow(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                run {
                    var visibleDropDown by remember { mutableStateOf(false) }
                    DropDownButton(
                        modifier = Modifier.padding(end = 4.dp),
                        visibleDropDown = visibleDropDown,
                        onDismissRequest = {
                            visibleDropDown = false
                        },
                        onClick = {
                            visibleDropDown = !visibleDropDown
                        },
                        item = {
                            Text(item.source.getDisplayText())
                        },
                        dropDown = {
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                immutableListOf(
                                    ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom,
                                    ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle,
                                    ImportedMailFilterCategoryScreenUiState.DataSource.MailHtml,
                                    ImportedMailFilterCategoryScreenUiState.DataSource.MailPlain,
                                    ImportedMailFilterCategoryScreenUiState.DataSource.Title,
                                    ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName,
                                ).forEach { source ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                visibleDropDown = false
                                                item.event.selectedSource(source)
                                            }
                                            .padding(8.dp),
                                    ) {
                                        Text(source.getDisplayText())
                                    }
                                }
                            }
                        },
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.padding(end = 4.dp),
                    text = "に",
                )
                run {
                    var visibleDropDown by remember { mutableStateOf(false) }
                    DropDownButton(
                        modifier = Modifier.padding(end = 4.dp),
                        item = {
                            Text(item.conditionType.getDisplayText())
                        },
                        visibleDropDown = visibleDropDown,
                        onDismissRequest = {
                            visibleDropDown = false
                        },
                        onClick = {
                            visibleDropDown = !visibleDropDown
                        },
                        dropDown = {
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                immutableListOf(
                                    ImportedMailFilterCategoryScreenUiState.ConditionType.Include,
                                    ImportedMailFilterCategoryScreenUiState.ConditionType.NotInclude,
                                    ImportedMailFilterCategoryScreenUiState.ConditionType.Equal,
                                    ImportedMailFilterCategoryScreenUiState.ConditionType.NotEqual,
                                ).forEach { type ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                visibleDropDown = false
                                                item.event.selectedConditionType(type)
                                            }
                                            .padding(8.dp),
                                    ) {
                                        Text(type.getDisplayText())
                                    }
                                }
                            }
                        },
                        contentDescription = null,
                    )
                }
                Text(
                    text = "とき",
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = item.text,
                    )
                    Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                }
                OutlinedButton(
                    modifier = Modifier.padding(8.dp),
                    onClick = { item.event.onClickTextChange() },
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
    visibleDropDown: Boolean,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    item: @Composable () -> Unit,
    dropDown: @Composable () -> Unit,
    contentDescription: String?,
) {
    Box(
        modifier = modifier,
    ) {
        OutlinedButton(
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
            onClick = {
                onClick()
            },
        ) {
            item()
            Icon(Icons.Default.ArrowDropDown, contentDescription)
        }
        DropdownMenu(
            expanded = visibleDropDown,
            onDismissRequest = onDismissRequest,
            focusable = true,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) {
                dropDown()
            }
        }
    }
}
