package net.matsudamper.money.frontend.common.viewmodel.importedmail.plain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailPlainScreenQuery
import net.matsudamper.money.frontend.graphql.TranslateTextMutation

public class ImportedMailPlainViewModel(
    private val id: ImportedMailId,
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val uiStateMutableFlow = MutableStateFlow(
        ImportedMailPlainScreenUiState(
            loadingState = ImportedMailPlainScreenUiState.LoadingState.Loading,
            event = object : ImportedMailPlainScreenUiState.Event {
                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickClose() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.backRequest()
                        }
                    }
                }

                override fun onClickRetry() {
                    fetch()
                }

                override fun onClickTranslate() {
                    translate()
                }
            },
        ),
    )

    public val uiStateFlow: StateFlow<ImportedMailPlainScreenUiState> = uiStateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val apolloResult = viewModelState.apolloResponseState

                if (apolloResult == null) {
                    uiStateMutableFlow.update {
                        it.copy(
                            loadingState = ImportedMailPlainScreenUiState.LoadingState.Loading,
                        )
                    }
                    return@collectLatest
                }

                if (apolloResult.isFailure) {
                    uiStateMutableFlow.update {
                        it.copy(
                            loadingState = ImportedMailPlainScreenUiState.LoadingState.Error,
                        )
                    }
                    return@collectLatest
                }

                val response = apolloResult.getOrThrow()
                val mailData = response.data?.user?.importedMailAttributes?.mail

                if (response.hasErrors() || mailData == null) {
                    uiStateMutableFlow.update {
                        it.copy(
                            loadingState = ImportedMailPlainScreenUiState.LoadingState.Error,
                        )
                    }
                    return@collectLatest
                }

                val html = sequence {
                    yield(
                        mailData.plain
                            ?.replace("\r\n", "<br>")
                            ?.replace("\n", "<br>"),
                    )
                }.filterNotNull().firstOrNull().orEmpty()

                val currentTranslationState = when (val currentState = uiStateMutableFlow.value.loadingState) {
                    is ImportedMailPlainScreenUiState.LoadingState.Loaded -> currentState.translationState
                    else -> ImportedMailPlainScreenUiState.TranslationState.NotTranslated
                }

                uiStateMutableFlow.update {
                    it.copy(
                        loadingState = ImportedMailPlainScreenUiState.LoadingState.Loaded(
                            html = html,
                            translationState = currentTranslationState,
                        ),
                    )
                }
            }
        }
    }

    private fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                graphqlClient.apolloClient.query(
                    ImportedMailPlainScreenQuery(
                        id = id,
                    ),
                )
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
            }

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    apolloResponseState = result,
                )
            }
        }
    }

    private fun translate() {
        val currentLoadedState = uiStateMutableFlow.value.loadingState as? ImportedMailPlainScreenUiState.LoadingState.Loaded
            ?: return

        val plainText = currentLoadedState.html
            .replace("<br>", "\n")
            .replace(Regex("<[^>]+>"), "")

        uiStateMutableFlow.update {
            it.copy(
                loadingState = currentLoadedState.copy(
                    translationState = ImportedMailPlainScreenUiState.TranslationState.Loading,
                ),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                graphqlClient.apolloClient.mutation(
                    TranslateTextMutation(
                        text = plainText,
                        targetLanguage = "ja",
                    ),
                ).execute()
            }

            val newTranslationState = result.fold(
                onSuccess = { response ->
                    val translateResult = response.data?.userMutation?.translateText
                    if (translateResult != null) {
                        val translatedHtml = translateResult.translatedText
                            .replace("\n", "<br>")
                        ImportedMailPlainScreenUiState.TranslationState.Translated(
                            translatedHtml = translatedHtml,
                            sourceLanguage = translateResult.sourceLanguage,
                            targetLanguage = translateResult.targetLanguage,
                        )
                    } else {
                        ImportedMailPlainScreenUiState.TranslationState.Error
                    }
                },
                onFailure = {
                    ImportedMailPlainScreenUiState.TranslationState.Error
                },
            )

            val latestLoadedState = uiStateMutableFlow.value.loadingState as? ImportedMailPlainScreenUiState.LoadingState.Loaded
                ?: return@launch

            uiStateMutableFlow.update {
                it.copy(
                    loadingState = latestLoadedState.copy(
                        translationState = newTranslationState,
                    ),
                )
            }
        }
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val apolloResponseState: Result<ApolloResponse<ImportedMailPlainScreenQuery.Data>>? = null,
    )
}
