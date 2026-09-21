package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.base.ColorUtil
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.ScreenBackHandler
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_chevron_right
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_close
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_edit
import net.matsudamper.money.frontend.common.ui.lib.StatusBarAppearance
import org.jetbrains.compose.resources.painterResource

public data class SettingSubCategoryScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public sealed interface HeroMode {
        public data object Base : HeroMode

        public data object EditingSubCategoryName : HeroMode
    }

    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val subCategoryName: String,
            val categoryName: String,
            val categoryColor: Color?,
            val heroMode: HeroMode,
        ) : LoadingState
    }

    @Immutable
    public interface Event {
        public suspend fun onResume()

        public fun onClickBack()

        public fun onClickEditSubCategoryName()

        public fun onSubCategoryNameEditComplete(text: String)

        public fun onClickCategory()
    }
}

@Composable
public fun SettingSubCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: SettingSubCategoryScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }
    val loadedState = uiState.loadingState as? SettingSubCategoryScreenUiState.LoadingState.Loaded
    val isNameEditing = loadedState?.heroMode == SettingSubCategoryScreenUiState.HeroMode.EditingSubCategoryName

    ScreenBackHandler(enabled = isNameEditing) {
        uiState.event.onClickBack()
    }

    val heroColor = loadedState?.categoryColor ?: MaterialTheme.colorScheme.primary
    StatusBarAppearance(isLightStatusBar = ColorUtil.contrastTextColor(heroColor) == Color.Black)

    Column(modifier = modifier.fillMaxSize()) {
        HeroSection(
            modifier = Modifier.fillMaxWidth(),
            loadedState = loadedState,
            heroColor = heroColor,
            windowInsets = windowInsets,
            onClickBack = { uiState.event.onClickBack() },
            onClickEditSubCategoryName = { uiState.event.onClickEditSubCategoryName() },
            onSubCategoryNameEditComplete = { text -> uiState.event.onSubCategoryNameEditComplete(text) },
            onClickCategory = { uiState.event.onClickCategory() },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState.loadingState) {
                is SettingSubCategoryScreenUiState.LoadingState.Loading -> {
                    CircularProgressIndicator()
                }

                is SettingSubCategoryScreenUiState.LoadingState.Error -> {
                    Text(
                        text = "データの取得に失敗しました",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is SettingSubCategoryScreenUiState.LoadingState.Loaded -> Unit
            }
        }
    }
}

@Composable
private fun HeroSection(
    modifier: Modifier,
    loadedState: SettingSubCategoryScreenUiState.LoadingState.Loaded?,
    heroColor: Color,
    windowInsets: PaddingValues,
    onClickBack: () -> Unit,
    onClickEditSubCategoryName: () -> Unit,
    onSubCategoryNameEditComplete: (String) -> Unit,
    onClickCategory: () -> Unit,
) {
    val subCategoryName = loadedState?.subCategoryName.orEmpty()
    val isEditMode = loadedState?.heroMode == SettingSubCategoryScreenUiState.HeroMode.EditingSubCategoryName
    var editingText by rememberSaveable(subCategoryName, isEditMode) { mutableStateOf(subCategoryName) }

    Surface(
        modifier = modifier,
        shape = RectangleShape,
        contentColor = ColorUtil.contrastTextColor(heroColor).copy(alpha = 0.75f),
        color = heroColor,
    ) {
        ProvideTextStyle(
            LocalTextStyle.current.merge(
                color = LocalContentColor.current,
            ),
        ) {
            Column(
                modifier = Modifier.padding(top = windowInsets.calculateTopPadding()),
            ) {
                HeroTopBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp),
                    isEditMode = isEditMode,
                    editingText = editingText,
                    onClickBack = onClickBack,
                    onSubCategoryNameEditComplete = onSubCategoryNameEditComplete,
                )

                if (loadedState != null) {
                    HeroBody(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                        subCategoryName = loadedState.subCategoryName,
                        categoryName = loadedState.categoryName,
                        isEditMode = isEditMode,
                        editingText = editingText,
                        onEditingTextChange = { editingText = it },
                        onClickEditSubCategoryName = onClickEditSubCategoryName,
                        onClickCategory = onClickCategory,
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroTopBar(
    modifier: Modifier,
    isEditMode: Boolean,
    editingText: String,
    onClickBack: () -> Unit,
    onSubCategoryNameEditComplete: (String) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClickBack,
        ) {
            Icon(
                painter = painterResource(if (isEditMode) Res.drawable.ic_close else Res.drawable.ic_arrow_back),
                contentDescription = if (isEditMode) "キャンセル" else "戻る",
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = "サブカテゴリ",
            style = MaterialTheme.typography.titleLarge,
        )

        if (isEditMode) {
            TextButton(
                onClick = { onSubCategoryNameEditComplete(editingText) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalContentColor.current,
                ),
            ) {
                Text(
                    text = "完了",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HeroBody(
    modifier: Modifier,
    subCategoryName: String,
    categoryName: String,
    isEditMode: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onClickEditSubCategoryName: () -> Unit,
    onClickCategory: () -> Unit,
) {
    Column(modifier = modifier) {
        CategoryBadge(
            categoryName = categoryName,
            onClick = onClickCategory,
        )

        Spacer(Modifier.height(10.dp))

        if (isEditMode) {
            Text(
                text = "サブカテゴリ名",
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(
                        width = 2.dp,
                        color = LocalContentColor.current,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    modifier = Modifier.weight(1f),
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LocalContentColor.current,
                    ),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    singleLine = true,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subCategoryName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    modifier = Modifier.size(36.dp),
                    onClick = onClickEditSubCategoryName,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = "サブカテゴリ名を変更",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    categoryName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LocalContentColor.current.copy(alpha = 0.25f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
@Preview
private fun SubCategorySettingScreenPreview() {
    val uiState = SettingSubCategoryScreenUiState(
        event = object : SettingSubCategoryScreenUiState.Event {
            override suspend fun onResume() {}
            override fun onClickBack() {}
            override fun onClickEditSubCategoryName() {}
            override fun onSubCategoryNameEditComplete(text: String) {}
            override fun onClickCategory() {}
        },
        loadingState = SettingSubCategoryScreenUiState.LoadingState.Loaded(
            subCategoryName = "スーパー",
            categoryName = "食費",
            categoryColor = Color(0xFF4F8F2C),
            heroMode = SettingSubCategoryScreenUiState.HeroMode.Base,
        ),
        kakeboScaffoldListener = object : KakeboScaffoldListener {
            override fun onClickTitle() {}
        },
    )

    AppRoot {
        SettingSubCategoryScreen(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxSize(),
            uiState = uiState,
            windowInsets = PaddingValues(0.dp),
        )
    }
}
