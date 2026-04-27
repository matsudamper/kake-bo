package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_add
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_check_circle
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_close
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_computer
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_delete
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_devices
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_edit
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_expand_less
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_expand_more
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_key
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_lock
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_logout
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_more_vert
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_shield
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_smartphone
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

public data class LoginSettingScreenUiState(
    val textInputDialogState: TextInputDialogState?,
    val confirmDialog: ConfirmDialog?,
    val loadingState: LoadingState,
    val event: Event,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {

    @Immutable
    public interface ConfirmDialog {
        public val title: String
        public val description: String?

        public fun onConfirm()

        public fun onDismiss()
    }

    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val fidoList: ImmutableList<Fido>,
            val password: Password,
            val currentSession: Session,
            val sessionList: ImmutableList<Session>,
        ) : LoadingState
    }

    public data class Session(
        val name: String,
        val lastAccess: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDelete()

            public fun onClickNameChange()
        }
    }

    public data class Password(
        val isRegistered: Boolean,
        val maskedDisplay: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickChangePassword()

            public fun onClickDeletePassword()
        }
    }

    public data class TextInputDialogState(
        val title: String,
        val text: String,
        val type: TextFieldType,
        val onConfirm: (String) -> Unit,
        val onCancel: () -> Unit,
    )

    public data class Fido(
        val name: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDelete()
        }
    }

    @Immutable
    public interface Event {
        public fun onClickBack()

        public fun onClickAddFido()

        public fun onClickLogout()
    }
}

@Composable
public fun LoginSettingScreen(
    uiState: LoginSettingScreenUiState,
    modifier: Modifier = Modifier,
    windowInsets: PaddingValues,
) {
    if (uiState.textInputDialogState != null) {
        FullScreenTextInput(
            title = uiState.textInputDialogState.title,
            default = uiState.textInputDialogState.text,
            inputType = uiState.textInputDialogState.type,
            onComplete = { uiState.textInputDialogState.onConfirm(it) },
            canceled = { uiState.textInputDialogState.onCancel() },
        )
    }
    uiState.confirmDialog?.also { dialog ->
        AlertDialog(
            title = { Text(dialog.title) },
            description = dialog.description?.let { description ->
                { Text(description) }
            },
            onClickPositive = { dialog.onConfirm() },
            onClickNegative = { dialog.onDismiss() },
            onDismissRequest = { dialog.onDismiss() },
        )
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
                    Text(text = "ログイン設定")
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        when (uiState.loadingState) {
            is LoginSettingScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    uiState = uiState.loadingState,
                    event = uiState.event,
                )
            }

            is LoginSettingScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

private enum class LoginSettingTab {
    SecurityKey,
    Password,
    Session,
}

@Composable
private fun LoadedContent(
    uiState: LoginSettingScreenUiState.LoadingState.Loaded,
    event: LoginSettingScreenUiState.Event,
    modifier: Modifier = Modifier,
) {
    if (LocalIsLargeScreen.current) {
        TabletLoadedContent(
            uiState = uiState,
            event = event,
            modifier = modifier,
        )
    } else {
        PhoneLoadedContent(
            uiState = uiState,
            event = event,
            modifier = modifier,
        )
    }
}

@Composable
private fun PhoneLoadedContent(
    uiState: LoginSettingScreenUiState.LoadingState.Loaded,
    event: LoginSettingScreenUiState.Event,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxContentWidth = 700.dp
        val width = maxWidth
        val horizontalPadding = remember(width, maxContentWidth) {
            val basePadding = 16.dp
            if (maxContentWidth > width) {
                basePadding
            } else {
                (width - maxContentWidth) / 2 + basePadding
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 16.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                ExpandableSettingCard(
                    leadingIconRes = Res.drawable.ic_shield,
                    title = "セキュリティーキー",
                    badgeCount = uiState.fidoList.size,
                ) {
                    FidoCardContent(
                        fidoList = uiState.fidoList,
                        onClickAddFido = { event.onClickAddFido() },
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                ExpandableSettingCard(
                    leadingIconRes = Res.drawable.ic_lock,
                    title = "パスワード",
                    badgeCount = null,
                ) {
                    PasswordCardContent(password = uiState.password)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                ExpandableSettingCard(
                    leadingIconRes = Res.drawable.ic_devices,
                    title = "セッション",
                    badgeCount = uiState.sessionList.size + 1,
                ) {
                    SessionCardContent(
                        currentSession = uiState.currentSession,
                        sessionList = uiState.sessionList,
                    )
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
            item {
                LogoutButton(onClick = { event.onClickLogout() })
            }
        }
    }
}

@Composable
private fun TabletLoadedContent(
    uiState: LoginSettingScreenUiState.LoadingState.Loaded,
    event: LoginSettingScreenUiState.Event,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(LoginSettingTab.SecurityKey) }
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabletNavRow(
                    leadingIconRes = Res.drawable.ic_shield,
                    title = "セキュリティーキー",
                    badgeCount = uiState.fidoList.size,
                    selected = selectedTab == LoginSettingTab.SecurityKey,
                    onClick = { selectedTab = LoginSettingTab.SecurityKey },
                )
                TabletNavRow(
                    leadingIconRes = Res.drawable.ic_lock,
                    title = "パスワード",
                    badgeCount = null,
                    selected = selectedTab == LoginSettingTab.Password,
                    onClick = { selectedTab = LoginSettingTab.Password },
                )
                TabletNavRow(
                    leadingIconRes = Res.drawable.ic_devices,
                    title = "セッション",
                    badgeCount = uiState.sessionList.size + 1,
                    selected = selectedTab == LoginSettingTab.Session,
                    onClick = { selectedTab = LoginSettingTab.Session },
                )
            }
            LogoutButton(onClick = { event.onClickLogout() })
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        ) {
            when (selectedTab) {
                LoginSettingTab.SecurityKey -> {
                    TabletDetailSection(title = "セキュリティーキー") {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            FidoCardContent(
                                fidoList = uiState.fidoList,
                                onClickAddFido = { event.onClickAddFido() },
                            )
                        }
                    }
                }

                LoginSettingTab.Password -> {
                    TabletDetailSection(title = "パスワード") {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            PasswordCardContent(password = uiState.password)
                        }
                    }
                }

                LoginSettingTab.Session -> {
                    TabletDetailSection(title = "セッション") {
                        Card(shape = RoundedCornerShape(20.dp)) {
                            SessionCardContent(
                                currentSession = uiState.currentSession,
                                sessionList = uiState.sessionList,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletNavRow(
    leadingIconRes: DrawableResource,
    title: String,
    badgeCount: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(leadingIconRes),
                contentDescription = null,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (badgeCount != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletDetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
}

@Composable
private fun ExpandableSettingCard(
    leadingIconRes: DrawableResource,
    title: String,
    badgeCount: Int?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(leadingIconRes),
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (badgeCount != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(if (expanded) Res.drawable.ic_expand_less else Res.drawable.ic_expand_more),
                contentDescription = if (expanded) "閉じる" else "開く",
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                content()
            }
        }
    }
}

@Composable
private fun FidoCardContent(
    fidoList: ImmutableList<LoginSettingScreenUiState.Fido>,
    onClickAddFido: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickAddFido() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "セキュリティーの追加",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        fidoList.forEach { fido ->
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            FidoItemRow(
                fido = fido,
            )
        }
    }
}

@Composable
private fun FidoItemRow(
    fido: LoginSettingScreenUiState.Fido,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(Res.drawable.ic_key),
            contentDescription = null,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = fido.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        IconCircleButton(
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            contentDescription = "削除",
            onClick = { fido.event.onClickDelete() },
            icon = painterResource(Res.drawable.ic_close),
        )
    }
}

@Composable
private fun IconCircleButton(
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit,
    icon: Painter,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = icon,
            tint = contentColor,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun PasswordCardContent(
    password: LoginSettingScreenUiState.Password,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(Res.drawable.ic_check_circle),
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (password.isRegistered) {
                    Text(
                        text = "パスワード設定済み",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = password.maskedDisplay,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = "パスワード未設定",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PasswordActionRow(
            iconRes = Res.drawable.ic_edit,
            text = "パスワードを変更",
            color = MaterialTheme.colorScheme.primary,
            onClick = { password.event.onClickChangePassword() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PasswordActionRow(
            iconRes = Res.drawable.ic_delete,
            text = "パスワードを削除",
            color = MaterialTheme.colorScheme.error,
            onClick = { password.event.onClickDeletePassword() },
        )
    }
}

@Composable
private fun PasswordActionRow(
    iconRes: DrawableResource,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            tint = color,
            contentDescription = null,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SessionCardContent(
    currentSession: LoginSettingScreenUiState.Session,
    sessionList: ImmutableList<LoginSettingScreenUiState.Session>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "現在のセッション")
        SessionRow(
            session = currentSession,
            leadingIconRes = Res.drawable.ic_smartphone,
            trailing = {
                IconButton(onClick = { currentSession.event.onClickNameChange() }) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(Res.drawable.ic_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "名前を変更",
                    )
                }
            },
        )
        if (sessionList.isNotEmpty()) {
            SectionLabel(text = "その他のセッション")
            sessionList.forEach { session ->
                SessionRow(
                    session = session,
                    leadingIconRes = Res.drawable.ic_computer,
                    trailing = {
                        OtherSessionMoreMenu(
                            onClickDelete = { session.event.onClickDelete() },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 4.dp),
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SessionRow(
    session: LoginSettingScreenUiState.Session,
    leadingIconRes: DrawableResource,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "最終アクセス: ${session.lastAccess}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        trailing()
    }
}

@Composable
private fun OtherSessionMoreMenu(
    onClickDelete: () -> Unit,
) {
    var visibleMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { visibleMenu = !visibleMenu }) {
        Icon(painter = painterResource(Res.drawable.ic_more_vert), contentDescription = "メニューを開く")
        if (visibleMenu) {
            Popup(
                onDismissRequest = { visibleMenu = false },
                properties = PopupProperties(focusable = true),
            ) {
                Card(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    visibleMenu = false
                                    onClickDelete()
                                }
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.error,
                            text = "削除",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(Res.drawable.ic_logout),
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "ログアウト",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
