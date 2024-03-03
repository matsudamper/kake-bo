package net.matsudamper.money.frontend.common.base.nav.user

internal class UrlPlaceHolderParser<D : DirectionUrl>(
    private val directions: List<D>,
) {
    fun parse(pathname: String): ScreenState<D> {
        val result =
            directions.map direction@{ screen ->
                var reamingPathname = pathname

                val keyValues = mutableListOf<Pair<String, String?>>()
                val placeholderRegex = """^\{(.+?)\}$""".toRegex()
                val result =
                    splitUrl(screen.placeholderUrl).asSequence()
                        .map { phrase ->
                            val placeholderValue =
                                placeholderRegex.find(phrase)
                                    ?.groupValues?.getOrNull(1)

                            if (placeholderValue != null) {
                                keyValues.add(placeholderValue to null)
                                return@map true
                            } else {
                                val index =
                                    reamingPathname.indexOf(phrase)
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
                                    val key =
                                        keyValues.dropLast(0).getOrNull(0)?.first
                                            ?: return@map false

                                    keyValues.add(key to value)
                                    reamingPathname = reamingPathname.drop(index + phrase.length)
                                }
                                return@map true
                            }
                        }.all { it }

                val lastIsPlaceholder: Boolean
                val last = keyValues.lastOrNull()
                if (reamingPathname.isNotEmpty() && last != null && last.second == null) {
                    keyValues.add(last.first to reamingPathname)
                    reamingPathname = ""
                    lastIsPlaceholder = true
                } else {
                    lastIsPlaceholder = false
                }
                ParseResult(
                    success = result,
                    keyValues = keyValues,
                    screen = screen,
                    reamingPath = reamingPathname,
                    lastIsPlaceholder = lastIsPlaceholder,
                )
            }.filter {
                it.success && it.reamingPath.isEmpty()
            }.minByOrNull {
                // "/hoge/100/piyo"において、1よりも2を優先する
                // 1: /hoge/{fuga} -> fuga=100/piyo
                // 2: /hoge/{fuga}/piyo -> fuga=100
                if (it.lastIsPlaceholder) {
                    1
                } else {
                    0
                }
            }

        return if (result != null) {
            ScreenState(
                screen = result.screen,
                pathParams =
                    result.keyValues
                        .reversed()
                        .distinctBy { it.first }
                        .associate { (key, value) -> key to value.orEmpty() },
            )
        } else {
            ScreenState(
                screen = null,
                pathParams = mapOf(),
            )
        }
    }

    private fun splitUrl(url: String): List<String> {
        val results =
            """\{(.+?)\}""".toRegex()
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

    data class ScreenState<out D : DirectionUrl>(
        public val screen: D?,
        public val pathParams: Map<String, String>,
    )

    private data class ParseResult<D : DirectionUrl>(
        val success: Boolean,
        val keyValues: List<Pair<String, String?>>,
        val screen: D,
        val reamingPath: String,
        val lastIsPlaceholder: Boolean,
    )
}
