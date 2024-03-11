package net.matsudamper.money.frontend.common.ui.screen.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NotFoundScreen(paddingValues: PaddingValues) {
    Scaffold(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) { paddingValues1 ->
        Box(Modifier.padding(paddingValues1)) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Page Not Found",
            )
        }
    }
}
