package net.matsudamper.money

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.CustomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomTheme {
                Content(
                    modifier = Modifier.fillMaxSize(),
                    globalEventSender = globalEventSender,
                    composeSizeProvider = { composeSize },
                )
            }
        }
    }
}
