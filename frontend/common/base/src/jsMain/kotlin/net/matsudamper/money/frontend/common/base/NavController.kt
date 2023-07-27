package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window

@Immutable
@Suppress("RegExpRedundantEscape")
public class ScreenNavControllerImpl(
    initial: Screen,
    private val directions: List<Screen>,
) : ScreenNavController {
    private var screenState: ScreenState by mutableStateOf(
        ScreenState(
            url = initial.url,
            screen = initial,
        ),
    )
    override val currentNavigation: Screen get() = screenState.screen

    init {
        updateNavigation()

        window.addEventListener(
            "popstate",
            callback = { updateNavigation() },
        )
    }

    private fun updateNavigation() {
        val result = directions.map { screen ->
            var pathname = window.location.pathname

            val keyValues = mutableListOf<Pair<String, String?>>()
            val placeholderRegex = """^\{(.+?)\}$""".toRegex()
            val result = splitUrl(screen.url)
                .map { phrase ->
                    val placeholderValue = placeholderRegex.find(phrase)
                        ?.groupValues?.getOrNull(1)

                    if (placeholderValue != null) {
                        keyValues.add(placeholderValue to null)
                        return@map true
                    } else {
                        val index = pathname.indexOf(phrase)
                            .takeIf { it >= 0 } ?: return@map false
                        println("$pathname $phrase -> $index")

                        if (index == 0) {
                            val lastKeyValue = keyValues.lastOrNull()
                            if (lastKeyValue != null && lastKeyValue.second == null) {
                                // placeholderを飛ばした
                                return@map false
                            }

                            pathname = pathname.drop(phrase.length)
                        } else {
                            val value = pathname.substring(0, index)
                            val key = keyValues.dropLast(1).getOrNull(0)?.first ?: return@map false

                            keyValues.add(key to value)
                            pathname = pathname.drop(index)
                        }
                        return@map true
                    }
                }.all { it }

            val last = keyValues.lastOrNull()
            if (pathname.isNotEmpty() && last != null) {
                keyValues.add(last.first to pathname)
                pathname = ""
            }
            ParseResult(
                success = result,
                keyValues = keyValues,
                screen = screen,
                reamingPath = pathname,
            )
        }.firstOrNull { it.success && it.reamingPath.isEmpty() }

        if (result != null) {
            println(JSON.stringify(result.toString()))
            screenState = ScreenState(
                url = result.screen.url,
                screen = result.screen,
            )
        } else {
            throw IllegalStateException("404")
        }
    }

    public data class ParseResult(
        val success: Boolean,
        val keyValues: List<Pair<String, String?>>,
        val screen: Screen,
        val reamingPath: String,
    )

    override fun <T : Screen> navigate(
        navigation: T,
        urlBuilder: (T) -> String,
    ) {
        val url = urlBuilder(navigation)
        window.history.pushState(
            data = TAG,
            title = navigation.title,
            url = url,
        )
        println("navigate: $navigation, $url")
        screenState = ScreenState(
            url = url,
            screen = navigation,
        )
    }

    override fun back() {
        window.history.back()
    }

    private fun splitUrl(url: String): List<String> {
        val results = """\{(.+?)\}""".toRegex()
            .findAll(url)

        return results.fold(listOf<Int>()) { result, matchResult ->
            result
                .plus(matchResult.range.first)
                .plus(matchResult.range.last + 1)
        }
            .let {
                buildList {
                    add(0)
                    addAll(it)
                    add(url.length)
                }
            }.distinct()
            .let {
                it.zipWithNext()
                    .map { (start, end) ->
                        url.substring(start, end)
                    }
            }
    }

    private class ScreenState(
        val url: String,
        val screen: Screen,
    )

    public companion object {
        private const val TAG = "FHAOHWO!!O@&*DAOH(GA&&(DA&("
    }
}
