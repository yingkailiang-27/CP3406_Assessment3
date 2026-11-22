package edu.algosteps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.algosteps.data.StudyRepository
import edu.algosteps.data.SubmissionMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubmissionSummary(
    val lessonId: String,
    val passed: Int,
    val total: Int,
    val submittedAt: Long,
    val mode: SubmissionMode = SubmissionMode.LEGACY
) {
    val accepted get() = total > 0 && passed == total
}

data class DashboardStats(val submissions: List<SubmissionSummary>) {
    val accepted = submissions.count { it.accepted }
    val solvedTopics = submissions.filter { it.accepted }.map { it.lessonId }.toSet()
}

class DashboardViewModel(private val repository: StudyRepository) : ViewModel() {
    private val subscribed = SharingStarted.WhileSubscribed(5_000)
    val stats = repository.attempts.map { rows ->
        DashboardStats(rows.map { SubmissionSummary(it.lessonId, it.passed, it.total, it.submittedAt, SubmissionMode.valueOf(it.mode)) })
    }.stateIn(viewModelScope, subscribed, null)
    val preferences = repository.preferences.stateIn(viewModelScope, subscribed, null)
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }
    fun setHints(enabled: Boolean) = write { repository.setShowHints(enabled) }
    fun setReminders(enabled: Boolean) = write { repository.setReminders(enabled) }
    fun clearProgress() = write {
        awaitDraftSaves(repository)
        repository.clearProgress()
        _message.value = "Submissions and drafts cleared."
    }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _message.value = "Could not save this change. Please try again."
            }
        }
    }

    companion object {
        fun factory(repository: StudyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
