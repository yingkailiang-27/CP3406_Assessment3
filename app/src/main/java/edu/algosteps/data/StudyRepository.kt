package edu.algosteps.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy {
        Room.databaseBuilder(appContext, StudyDatabase::class.java, "algo-steps.db")
            .addMigrations(StudyDatabase.MIGRATION_1_2).build()
    }
    val repository: StudyRepository by lazy {
        StudyRepository(database.studyDao(), WikipediaSource(), DeviceReminders(appContext))
    }
}

fun interface ReadingSource {
    /** Returns null if the service has no material for this topic. */
    suspend fun fetch(lessonId: String): Reading?
}

interface ReminderScheduler {
    suspend fun update(enabled: Boolean)
    fun dismiss()
}

class StudyRepository(
    private val dao: StudyDao,
    private val source: ReadingSource,
    private val scheduler: ReminderScheduler,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val attempts: Flow<List<Attempt>> = dao.attempts()
    val preferences: Flow<StudyPreferences> = dao.preferences().map { it ?: StudyPreferences() }
    private val remindersLock = Mutex()

    suspend fun latestAttempt(lessonId: String, mode: SubmissionMode): Attempt? =
        dao.latestAttempt(lessonId, mode.name)

    suspend fun saveAttempt(
        lessonId: String,
        passed: Int,
        total: Int,
        mode: SubmissionMode = SubmissionMode.PRACTICE,
        sourceHash: String? = null
    ): Attempt {
        require(total > 0 && passed in 0..total) { "Invalid test result." }
        val saved = dao.recordAttempt(Attempt(lessonId, passed, total, clock(), mode = mode.name, sourceHash = sourceHash))
        // The insert is the grading commit. A stale reminder must not turn a saved
        // score into a failed submission that the student is asked to retry.
        if (passed == total) {
            try {
                remindersLock.withLock {
                    if (dao.pendingLesson() == null) scheduler.dismiss()
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (ignored: Exception) {
                android.util.Log.w("AlgoSteps", "Could not clear a review reminder.", ignored)
            }
        }
        return saved
    }

    suspend fun saveDraft(lessonId: String, source: String) = dao.putDraft(Draft(lessonId, source))
    suspend fun loadDraft(lessonId: String): String? = dao.draft(lessonId)
    suspend fun setShowHints(enabled: Boolean) = dao.setHints(enabled)

    suspend fun setReminders(enabled: Boolean) = remindersLock.withLock {
        // For opt-out, revoke consent first so a worker already running sees the change.
        if (!enabled) {
            dao.setReminderPreference(false)
            scheduler.dismiss()
        }
        scheduler.update(enabled)
        // A new periodic request first becomes eligible the following day.
        if (enabled) dao.setReminderPreference(true)
    }

    suspend fun pendingReview(): String? {
        if (!preferences.first().reminders) return null
        return dao.pendingLesson()
    }

    // Consent revocation and progress deletion dismiss on the same lock. A worker
    // which has already read an eligible lesson cannot post after that dismissal.
    internal suspend fun deliverReviewIfDue(post: () -> Unit) = remindersLock.withLock {
        if (pendingReview() != null) post()
    }

    suspend fun clearProgress() = remindersLock.withLock {
        dao.clearProgress()
        scheduler.dismiss()
    }

    fun reading(lessonId: String): Flow<Reading?> = dao.reading(lessonId)

    suspend fun refreshReading(lessonId: String): Boolean {
        val reading = try {
            source.fetch(lessonId)
        } catch (_: IOException) {
            null
        } ?: return false
        dao.putReading(reading)
        return true
    }
}
