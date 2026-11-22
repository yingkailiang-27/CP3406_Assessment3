package edu.algosteps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.algosteps.content.Case
import edu.algosteps.content.Lesson
import edu.algosteps.data.Attempt
import edu.algosteps.data.SubmissionMode
import edu.algosteps.data.StudyRepository
import java.security.MessageDigest
import edu.algosteps.judge.CodeJudge
import edu.algosteps.judge.JudgeException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SampleResult(
    val example: Case,
    val actual: Int?,
    val error: String? = null
) {
    val passed: Boolean get() = error == null && actual == example.expected
}

internal data class Evaluation(
    val samples: List<SampleResult>,
    val additionalPassed: Int,
    val additionalTotal: Int,
    val submitted: Boolean
) {
    val passed: Int get() = samples.count { it.passed } + additionalPassed
    val total: Int get() = samples.size + additionalTotal
}

internal data class CodingState(
    val source: String = "",
    val ready: Boolean = false,
    val running: Boolean = false,
    val report: Evaluation? = null,
    val savedReport: Evaluation? = null,
    val lastAttempt: Attempt? = null,
    val validationMessage: String? = null,
    val storageMessage: String? = null,
    val draftMessage: String? = null
) {
    val alreadySubmitted: Boolean get() =
        lastAttempt?.sourceHash?.let { it == sourceDigest(source) } ?: false
}

internal fun sourceDigest(source: String): String =
    MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

internal class CodingViewModel(
    private val lesson: Lesson,
    private val examMode: Boolean,
    private val repository: StudyRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(CodingState())
    val state = mutableState.asStateFlow()
    val preferences = repository.preferences
    private val mode = if (examMode) SubmissionMode.EXAM else SubmissionMode.PRACTICE
    private val starter = if (examMode) "def solve(nums, target):\n    pass" else lesson.starter
    private val draftKey = if (examMode) "${lesson.id}:exam" else lesson.id

    init {
        viewModelScope.launch {
            try {
                val draft = DraftWrites.load(repository, draftKey)
                mutableState.value = CodingState(
                    source = draft ?: starter, ready = true, lastAttempt = repository.latestAttempt(lesson.id, mode)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = CodingState(
                    source = starter, ready = true,
                    draftMessage = "Your saved draft could not be opened. The starter is shown."
                )
            }
        }
    }

    fun edit(source: String) {
        val previous = mutableState.value
        if (!previous.ready || previous.running || previous.source == source) return
        mutableState.value = previous.copy(
            source = source, report = null, savedReport = null, validationMessage = null,
            storageMessage = null, draftMessage = null
        )
        persist(source)
    }

    fun retryDraft() = persist(mutableState.value.source)

    private fun persist(source: String) {
        DraftWrites.save(repository, draftKey, source) { ok ->
            mutableState.update { state ->
                if (state.source != source) state else state.copy(
                    draftMessage = if (ok) null else "Couldn't save your latest edit on this device."
                )
            }
        }
    }

    fun evaluate(submit: Boolean) {
        val previous = mutableState.value
        if (!previous.ready || previous.running || previous.source.isBlank()) return
        if (submit && previous.alreadySubmitted) return
        val source = previous.source
        mutableState.value = previous.copy(running = true, storageMessage = null, validationMessage = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val program = CodeJudge.compile(source)
                    if (submit && examMode && !program.hasReturnStatement) return@withContext null
                    val samples = lesson.samples.map { test(program, it) }
                    val additional = if (submit) lesson.checks else emptyList()
                    Evaluation(samples, additional.count { test(program, it).passed }, additional.size, submit)
                }
                if (result == null) {
                    mutableState.update { it.copy(
                        running = false, report = null,
                        validationMessage = "Add a return statement to your exam solution before submitting."
                    ) }
                    return@launch
                }
                var saved = previous.lastAttempt
                if (submit) {
                    try {
                        saved = withContext(NonCancellable) {
                            DraftWrites.flush(repository)
                            repository.saveDraft(draftKey, source)
                            repository.saveAttempt(lesson.id, result.passed, result.total, mode, sourceDigest(source))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mutableState.update { it.copy(
                            running = false, report = result,
                            storageMessage = "Grading finished, but the result was not saved. Submit again to retry."
                        ) }
                        return@launch
                    }
                }
                mutableState.update { it.copy(
                    running = false, report = result,
                    savedReport = if (submit) result else it.savedReport,
                    lastAttempt = saved
                ) }
            } catch (e: CancellationException) {
                throw e
            } catch (error: JudgeException) {
                mutableState.update { it.copy(
                    running = false, report = null,
                    validationMessage = "Fix your code before submitting. ${error.message.orEmpty()}"
                ) }
            } catch (_: Exception) {
                mutableState.update { it.copy(
                    running = false, storageMessage = "Grading stopped unexpectedly. Please run your code again."
                ) }
            }
        }
    }

    private fun test(program: CodeJudge.Program, case: Case): SampleResult = try {
        SampleResult(case, program.run(case.nums, case.target))
    } catch (error: JudgeException) {
        SampleResult(case, null, error.message ?: "The code could not run.")
    }
}

/** Before deleting progress, allow earlier edits from a closed screen to reach Room. */
internal suspend fun awaitDraftSaves(repository: StudyRepository) = DraftWrites.flush(repository)

// Admissions happen synchronously on edit. An active writer coalesces rapid edits and
// survives a navigation change, so reopening a problem can already see its newest draft.
private object DraftWrites {
    private data class Key(val repo: StudyRepository, val name: String)
    private class Edit(val source: String, val onSaved: (Boolean) -> Unit)
    private class Lane(var edit: Edit, val drained: CompletableDeferred<Unit> = CompletableDeferred())
    private val writes = mutableMapOf<Key, Lane>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun flush(repo: StudyRepository) {
        val pending = synchronized(writes) {
            writes.filterKeys { it.repo === repo }.values.map { it.drained }
        }
        pending.awaitAll()
    }

    suspend fun load(repo: StudyRepository, name: String): String? {
        synchronized(writes) { writes[Key(repo, name)]?.let { return it.edit.source } }
        return repo.loadDraft(name)
    }

    fun save(repo: StudyRepository, name: String, source: String, onSaved: (Boolean) -> Unit) {
        val key = Key(repo, name)
        val edit = Edit(source, onSaved)
        val lane = synchronized(writes) {
            writes[key]?.let { it.edit = edit; return }
            Lane(edit).also { writes[key] = it }
        }
        scope.launch {
            while (true) {
                val current = synchronized(writes) { lane.edit }
                val ok = try {
                    repo.saveDraft(name, current.source)
                    true
                } catch (_: Exception) {
                    // This process-scoped writer has no screen cancellation. A cancelled
                    // database operation is surfaced as an unsuccessful edit too.
                    false
                }
                val last = synchronized(writes) {
                    if (lane.edit === current) {
                        writes.remove(key)
                        true
                    } else false
                }
                if (last) {
                    lane.drained.complete(Unit)
                    current.onSaved(ok)
                    break
                }
            }
        }
    }
}
