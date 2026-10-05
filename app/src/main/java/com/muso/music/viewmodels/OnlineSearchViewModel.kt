package com.muso.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.filterExplicit
import com.zionhuang.innertube.pages.SearchSummaryPage
import com.muso.music.constants.HideExplicitKey
import com.muso.music.models.ItemsPage
import com.muso.music.utils.dataStore
import com.muso.music.utils.get
import com.muso.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        // Ultra-fast All tab: one retry, then an empty page. The
                        // old failure path left summaryPage null forever, which
                        // kept the All filter on an endless skeleton.
                        var result = YouTube.searchSummary(query)
                        if (result.isFailure) {
                            reportException(result.exceptionOrNull() ?: Exception("searchSummary failed"))
                            delay(1500)
                            result = YouTube.searchSummary(query)
                        }
                        summaryPage = result.getOrNull()
                            ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            ?: SearchSummaryPage(emptyList())
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube.search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] = ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )
                            }
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
            }
        }
    }
}
