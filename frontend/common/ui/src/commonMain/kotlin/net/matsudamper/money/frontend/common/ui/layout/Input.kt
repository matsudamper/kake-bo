package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_clear
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_search
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

@Composable
public fun Input(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = onTextChange,
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = rememberCustomFontFamily(),
            )
        },
        singleLine = true,
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_clear),
                        contentDescription = "clear",
                    )
                }
            } else {
                IconButton(onClick = onSearch) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_search),
                        contentDescription = "search",
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}
