package net.matsudamper.money.frontend.common.base.nav.user


internal class UrlPlaceHolderParser(
    private val directions: List<Screens>,
) {
    fun parse(
        pathname: String,
    ): ScreenState {
        val result = directions.map { screen ->
            var reamingPathname = pathname

            val keyValues = mutableListOf<Pair<String, String?>>()
            val placeholderRegex = """^\{(.+?)\}$""".toRegex()
            val result = splitUrl(screen.placeholderUrl)
                .map { phrase ->
                    val placeholderValue = placeholderRegex.find(phrase)
                        ?.groupValues?.getOrNull(1)

                    if (placeholderValue != null) {
                        keyValues.add(placeholderValue to null)
                        return@map true
                    } else {
                        val index = reamingPathname.indexOf(phrase)
                            .takeIf { it >= 0 } ?: return@map false

                        if (index == 0) {
                            val lastKeyValue = keyValues.lastOrNull()
                            if (lastKeyValue != null && lastKeyValue.second == null) {
                                // placeholderを飛ばした
                                return@map false
                            }
                            reamingPathname = reamingPathname.drop(phrase.length)
                        } else {
                            val value = reamingPathname.substring(0, index)
                            val key = keyValues.dropLast(1).getOrNull(0)?.first ?: return@map false

                            keyValues.add(key to value)
                            reamingPathname = reamingPathname.drop(index)
                        }
                        return@map true
                    }
                }.all { it }

            val last = keyValues.lastOrNull()
            if (reamingPathname.isNotEmpty() && last != null) {
                keyValues.add(last.first to reamingPathname)
                reamingPathname = ""
            }
            ParseResult(
                success = result,
                keyValues = keyValues,
                screen = screen,
                reamingPath = reamingPathname,
            )
        }.firstOrNull { it.success && it.reamingPath.isEmpty() }

        return if (result != null) {
            ScreenState(
                screen = result.screen,
                params = result.keyValues
                    .reversed()
                    .distinctBy { it.first }
                    .associate { (key, value) -> key to value.orEmpty() },
            )
        } else {
            ScreenState(
                screen = Screens.NotFound,
                params = mapOf(),
            )
        }
    }

    private fun splitUrl(url: String): List<String> {
        val results = """\{(.+?)\}""".toRegex()
            .findAll(url)
            .toList()

        if (results.isEmpty()) {
            return listOf(url)
        }

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

    data class ScreenState(
        public val screen: Screens,
        public val params: Map<String, String>,
    )

    private data class ParseResult(
        val success: Boolean,
        val keyValues: List<Pair<String, String?>>,
        val screen: Screens,
        val reamingPath: String,
    )
}
