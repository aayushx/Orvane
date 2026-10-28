package com.orvane.app.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.ai.AiClient
import com.orvane.app.core.nl.NaturalLanguageDateParser
import com.orvane.app.data.repo.TaskRepository
import com.orvane.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val ai: AiClient,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    /** Raw quick-add text as typed. */
    val text = MutableStateFlow("")

    /** Live NL parse of [text]. */
    val parsed = kotlinx.coroutines.flow.MutableStateFlow<NaturalLanguageDateParser.Parsed?>(null)

    /** State of the optional AI assist (only works when a key is configured). */
    val aiBusy = MutableStateFlow(false)
    val aiMessage = MutableStateFlow<String?>(null)

    /** Pending AI suggestion: title and optional subtasks. */
    val aiSuggestion = MutableStateFlow<AiClient.Suggestion?>(null)

    /**
     * True only when an AI endpoint AND key are configured. The quick-add sheet
     * hides the "AI assist" button entirely otherwise — Settings promise the
     * button only "when configured" (QA: the button was always visible and
     * always failed without configuration).
     */
    val aiConfigured: StateFlow<Boolean> = settingsRepo.settings
        .map { it.aiBaseUrl.isNotBlank() && it.aiApiKey.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), false)

    init {
        viewModelScope.launch {
            text.collect { raw ->
                parsed.value = if (raw.isBlank()) null else NaturalLanguageDateParser.parse(raw)
            }
        }
    }

    /** Applies an AI suggestion: replaces text and remembers the subtasks. */
    fun applySuggestion(title: String, subtasks: List<String>) {
        text.value = title
        pendingSubtasks = subtasks
    }

    internal var pendingSubtasks: List<String> = emptyList()
        private set

    /**
     * Runs the optional AI assist against the configured OpenAI-compatible
     * endpoint. Fails silently to a friendly message when unconfigured/offline.
     */
    fun askAi() {
        val raw = text.value
        if (raw.isBlank() || aiBusy.value) return
        viewModelScope.launch {
            aiBusy.value = true
            aiMessage.value = null
            val s = settingsRepo.current()
            val suggestion = ai.suggestTask(raw, s.aiBaseUrl, s.aiApiKey, s.aiModel)
            aiBusy.value = false
            if (suggestion == null) {
                aiMessage.value = if (s.aiBaseUrl.isBlank() || s.aiApiKey.isBlank()) {
                    "Add an AI base URL and key in Settings first."
                } else {
                    "AI request failed — check connection and key."
                }
            } else {
                aiSuggestion.value = suggestion
            }
        }
    }

    fun clearAi() {
        aiMessage.value = null
        aiSuggestion.value = null
    }

    /**
     * Wipes the in-progress draft — called on every sheet dismiss/cancel.
     * (QA: drafts survived dismissal and re-opened with stale, doubled text.
     * Only a hard process death clears the ViewModel, matching the intended
     * "retention for process death" behavior.)
     */
    fun clearDraft() {
        text.value = ""
        parsed.value = null
        pendingSubtasks = emptyList()
        aiSuggestion.value = null
        aiMessage.value = null
    }

    /** Saves the quick add into the default list, including AI subtasks if any. */
    fun save(onSaved: () -> Unit, onError: () -> Unit = {}) {
        val raw = text.value
        if (raw.isBlank()) return
        viewModelScope.launch {
            val created = repo.quickAdd(raw)
            if (created != null) {
                for (sub in pendingSubtasks.take(5)) {
                    repo.quickAdd(sub, listId = created.listId, isSubtaskOf = created.id)
                }
                text.value = ""
                parsed.value = null
                pendingSubtasks = emptyList()
                aiSuggestion.value = null
                aiMessage.value = null
                onSaved()
            } else {
                onError()
            }
        }
    }
}

