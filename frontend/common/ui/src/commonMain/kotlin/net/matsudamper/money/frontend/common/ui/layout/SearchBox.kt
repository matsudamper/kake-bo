package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_clear
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_search
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

@Composable
public fun SearchBox(
    text: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "検索",
) {
    SearchBoxSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBoxLeadingIcon()
            SearchBoxTextContent(
                text = text,
                placeholder = placeholder,
                editable = false,
            )
            if (text.isNotEmpty()) {
                SearchBoxClearButton(onClear = onClear)
            }
        }
    }
}

@Composable
public fun SearchBoxField(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "検索",
) {
    SearchBoxSurface(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBoxLeadingIcon()
            SearchBoxTextContent(
                text = text,
                placeholder = placeholder,
                editable = true,
                onTextChange = onTextChange,
                onSearch = onSearch,
            )
            if (text.isNotEmpty()) {
                SearchBoxClearButton(onClear = onClear)
            }
        }
    }
}

@Composable
private fun SearchBoxSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        content = content,
    )
}

@Composable
private fun SearchBoxLeadingIcon() {
    Icon(
        modifier = Modifier.padding(start = 12.dp),
        painter = painterResource(Res.drawable.ic_search),
        contentDescription = "search",
    )
}

@Composable
private fun RowScope.SearchBoxTextContent(
    text: String,
    placeholder: String,
    editable: Boolean,
    onTextChange: ((String) -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    val fontFamily = rememberCustomFontFamily()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = fontFamily,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val contentModifier = Modifier
        .weight(1f)
        .padding(
            horizontal = 8.dp,
            vertical = 8.dp,
        )
    if (editable) {
        BasicTextField(
            modifier = contentModifier,
            value = text,
            onValueChange = { onTextChange?.invoke(it) },
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                    )
                }
                innerTextField()
            },
        )
    } else {
        Text(
            modifier = contentModifier,
            text = text.ifEmpty { placeholder },
            style = textStyle,
        )
    }
}

@Composable
private fun SearchBoxClearButton(
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClear)
            .padding(8.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_clear),
            contentDescription = "clear",
        )
    }
}

@Preview
@Composable
private fun SearchBoxPreview() {
    AppRoot(isDarkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            SearchBox(
                modifier = Modifier.width(320.dp),
                text = "",
                onClick = {},
                onClear = {},
            )
            SearchBox(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(320.dp),
                text = "コンビニ",
                onClick = {},
                onClear = {},
            )
            SearchBoxField(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(320.dp),
                text = "",
                onTextChange = {},
                onSearch = {},
                onClear = {},
                placeholder = "メールを検索",
            )
            SearchBoxField(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(320.dp),
                text = "amazon",
                onTextChange = {},
                onSearch = {},
                onClear = {},
                placeholder = "メールを検索",
            )
        }
    }
}
