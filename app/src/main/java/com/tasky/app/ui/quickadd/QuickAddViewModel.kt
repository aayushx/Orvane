package com.tasky.app.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasky.app.core.nl.NaturalLanguageDateParser
import com.tasky.app.data.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    /** Raw quick-add text as typed. */
    val text = MutableStateFlow("")

    /** Live NL parse of [text]. */
    val parsed = kotlinx.coroutines.flow.MutableStateFlow<NaturalLanguageDateParser.Parsed?>(null)

    init {
        viewModelScope.launch {
            text.collect { raw ->
                parsed.value = if (raw.isBlank()) null else NaturalLanguageDateParser.parse(raw)
            }
        }
    }

    /** Saves the quick add into the default list; keeps the sheet open on failure via no-op. */
    fun save(onSaved: () -> Unit, onError: () -> Unit = {}) {
        val raw = text.value
        if (raw.isBlank()) return
        viewModelScope.launch {
            val created = repo.quickAdd(raw)
            if (created != null) {
                text.value = ""
                parsed.value = null
                onSaved()
            } else {
                onError()
            }
        }
    }
}
