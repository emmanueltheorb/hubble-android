package com.orb.hubble.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.orb.hubble.utils.MediaPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class PickMediaScreenViewModel @Inject constructor(
    @param: ApplicationContext private val context: Context
): ViewModel() {

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaPager = _selectedFolder
        .flatMapLatest { folder ->
            Pager(
                config = PagingConfig(
                    pageSize = 40,//30           // Items per page
                    prefetchDistance = 40,//60   // Load next page when 20 items away
                    enablePlaceholders = true,
                    //initialLoadSize = 60
                ),
                pagingSourceFactory = {
                    MediaPagingSource(
                        context = context,
                        selectedFolder = folder
                    )
                }
            ).flow.cachedIn(viewModelScope)
        }

    fun setSelectedFolder(folder: String?) {
        _selectedFolder.value = folder
    }
}

/*
so when i used config = PagingConfig(
            pageSize = 40,//30           // Items per page
            prefetchDistance = 20,//60   // Load next page when 20 items away
            enablePlaceholders = true,
            initialLoadSize = 60,
            //maxSize = 120            // CRITICAL: Drops items from memory beyond this limit
        ) i had no erros, but when i used config = PagingConfig(
            pageSize = 40,//30           // Items per page
            prefetchDistance = 20,//60   // Load next page when 20 items away
            enablePlaceholders = true,
            initialLoadSize = 60,
            maxSize = 120            // CRITICAL: Drops items from memory beyond this limit
        ) i got an IllegalArgumentException when i scrolled down but then scrolled up back

java.lang.IllegalArgumentException: Key "1003473858" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.
    at androidx.compose.ui.internal.InlineClassHelperKt.throwIllegalArgumentException(InlineClassHelper.kt:36)
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcompose(
 */