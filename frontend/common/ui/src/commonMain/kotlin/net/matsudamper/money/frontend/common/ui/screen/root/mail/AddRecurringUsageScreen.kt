package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

@Composable
public fun AddRecurringUsageScreen(
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text("定期使用用途")
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("定期使用用途の登録はGraphQL Mutation(addRecurringUsageRule)から実行できます。")
            Text("この画面の詳細入力UIは今後の対応です。")
        }
    }
}
