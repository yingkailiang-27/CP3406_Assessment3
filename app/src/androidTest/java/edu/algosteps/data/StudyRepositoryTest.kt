package edu.algosteps.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StudyRepositoryTest {
    private lateinit var database: StudyDatabase
    private lateinit var repository: StudyRepository
    private lateinit var reminders: RecordingReminders
    private var offline = false
    private val article = Reading("binary-search", "Binary search", "A sorted sequence...", "https://en.wikipedia.org/wiki/Binary_search", 7L)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), StudyDatabase::class.java
        ).build()
        reminders = RecordingReminders()
        val source = ReadingSource { lesson ->
            if (offline) throw IOException("offline")
            if (lesson == article.lessonId) article else null
        }
        repository = StudyRepository(database.studyDao(), source, reminders) { 1L }
    }

    @After
    fun close() = database.close()

    @Test
    fun offlineRefreshPreservesLastSuccessfulReading() = runBlocking {
        assertNull(repository.reading(article.lessonId).first())
        assertTrue(repository.refreshReading(article.lessonId))
        offline = true
        assertFalse(repository.refreshReading(article.lessonId))
        assertEquals(article, repository.reading(article.lessonId).first())
    }

    @Test
    fun reviewsRequireConsentAndIgnorePreviouslySolvedExercises() = runBlocking {
        repository.saveAttempt("linear-search", 1, 4)
        assertNull(repository.pendingReview())
        repository.setReminders(true)
        assertEquals("linear-search", repository.pendingReview())
        repository.saveAttempt("linear-search", 4, 4)
        repository.saveAttempt("linear-search", 2, 4)
        assertNull(repository.pendingReview())
        repository.saveAttempt("binary-search", 3, 5)
        assertEquals("binary-search", repository.pendingReview())
        repository.setReminders(false)
        assertNull(repository.pendingReview())
        assertEquals(listOf(true, false), reminders.schedulingChanges)
    }

    @Test
    fun disablingRemindersDuringDeliveryLeavesNoNotification() = runBlocking {
        // Pause the worker after it has read eligibility, while opt-out runs.
        val checked = CompletableDeferred<Unit>()
        val allowWorkerToReturn = CompletableDeferred<Unit>()
        var optedIn = true
        var visible = false
        val dao = object : StudyDao by database.studyDao() {
            override fun preferences() = flow { emit(StudyPreferences(reminders = optedIn)) }
            override suspend fun pendingLesson(): String {
                checked.complete(Unit)
                allowWorkerToReturn.await()
                return "binary-search"
            }
            override suspend fun setReminderPreference(enabled: Boolean) { optedIn = enabled }
        }
        val device = object : ReminderScheduler {
            override fun dismiss() { visible = false }
            override suspend fun update(enabled: Boolean) = Unit
        }
        val repo = StudyRepository(dao, ReadingSource { null }, device)
        val worker = async { repo.deliverReviewIfDue { visible = true } }
        checked.await()
        val disable = async(start = CoroutineStart.UNDISPATCHED) { repo.setReminders(false) }
        allowWorkerToReturn.complete(Unit)
        worker.await()
        disable.await()
        assertFalse(optedIn)
        assertFalse(visible)
    }

    @Test
    fun firstSubmissionAndIdenticalRetryReturnTheSamePersistedRow() = runBlocking {
        val first = repository.saveAttempt("binary-search", 3, 6, SubmissionMode.EXAM, "same-source")
        val retry = repository.saveAttempt("binary-search", 3, 6, SubmissionMode.EXAM, "same-source")
        assertTrue(first.id > 0L)
        assertEquals(first, retry)
        assertEquals(first, repository.attempts.first().single())
    }

    @Test
    fun deleteClearsPrivateWorkTogetherAndRetainsSettingsAndPublicReadings() = runBlocking {
        repository.refreshReading(article.lessonId)
        repository.setShowHints(false)
        repository.saveDraft("linear-search", "return 3")
        repository.saveDraft("linear-search", "")
        assertEquals("", repository.loadDraft("linear-search"))
        repository.saveAttempt("linear-search", 0, 3)
        repository.saveAttempt("binary-search", 2, 4)
        assertEquals(listOf("binary-search", "linear-search"), repository.attempts.first().map { it.lessonId })
        repository.clearProgress()
        assertTrue(repository.attempts.first().isEmpty())
        assertNull(repository.loadDraft("linear-search"))
        assertFalse(repository.preferences.first().showHints)
        assertEquals(article, repository.reading(article.lessonId).first())
    }

    @Test
    fun migrationPreservesOldScoresAndDrafts() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-1-2-test.db"
        context.deleteDatabase(name)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE attempts (lessonId TEXT NOT NULL, passed INTEGER NOT NULL, total INTEGER NOT NULL, submittedAt INTEGER NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
                old.execSQL("CREATE INDEX index_attempts_lessonId ON attempts (lessonId)")
                old.execSQL("CREATE TABLE drafts (lessonId TEXT NOT NULL PRIMARY KEY, source TEXT NOT NULL)")
                old.execSQL("CREATE TABLE preferences (id INTEGER NOT NULL PRIMARY KEY, showHints INTEGER NOT NULL, reminders INTEGER NOT NULL)")
                old.execSQL("CREATE TABLE readings (lessonId TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, extract TEXT NOT NULL, url TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO attempts (lessonId, passed, total, submittedAt) VALUES ('binary-search', 2, 8, 100)")
                old.execSQL("INSERT INTO drafts VALUES ('binary-search', 'saved draft')")
                old.version = 1
            }
            val migrated = Room.databaseBuilder(context, StudyDatabase::class.java, name)
                .addMigrations(StudyDatabase.MIGRATION_1_2).build()
            try {
                    val dao = migrated.studyDao()
                    val attempt = dao.attempts().first().single()
                    assertEquals(SubmissionMode.LEGACY.name, attempt.mode)
                    assertEquals(2, attempt.passed)
                    assertNull(attempt.sourceHash)
                    assertEquals("saved draft", dao.draft("binary-search"))
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }

    private class RecordingReminders : ReminderScheduler {
        val schedulingChanges = mutableListOf<Boolean>()
        override suspend fun update(enabled: Boolean) { schedulingChanges.add(enabled) }
        override fun dismiss() = Unit
    }
}
