package edu.algosteps.data

import androidx.room.ColumnInfo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

enum class SubmissionMode(val label: String) {
    LEGACY("Previous version"), PRACTICE("Practice"), EXAM("Exam")
}

@Entity(tableName = "attempts", indices = [Index("lessonId")])
data class Attempt(
    val lessonId: String,
    val passed: Int,
    val total: Int,
    val submittedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "'LEGACY'") val mode: String = SubmissionMode.LEGACY.name,
    val sourceHash: String? = null
)

@Entity(tableName = "drafts")
data class Draft(@PrimaryKey val lessonId: String, val source: String)

@Entity(tableName = "preferences")
data class StudyPreferences(
    @PrimaryKey val id: Int = 1,
    val showHints: Boolean = true,
    val reminders: Boolean = false
)

@Entity(tableName = "readings")
data class Reading(
    @PrimaryKey val lessonId: String,
    val title: String,
    val extract: String,
    val url: String,
    val updatedAt: Long
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM attempts ORDER BY submittedAt DESC, id DESC")
    fun attempts(): Flow<List<Attempt>>

    // An exercise remains eligible until ANY submission has passed all checks.
    @Query("""
        SELECT a.lessonId FROM attempts AS a
        WHERE NOT EXISTS (
            SELECT 1 FROM attempts AS solved
            WHERE solved.lessonId = a.lessonId AND solved.passed = solved.total
        )
        ORDER BY a.submittedAt DESC, a.id DESC LIMIT 1
    """)
    suspend fun pendingLesson(): String?

    @Query("SELECT * FROM attempts WHERE lessonId = :lessonId AND mode = :mode ORDER BY id DESC LIMIT 1")
    suspend fun latestAttempt(lessonId: String, mode: String): Attempt?

    @Insert
    suspend fun addAttempt(attempt: Attempt): Long

    @Transaction
    suspend fun recordAttempt(attempt: Attempt): Attempt {
        if (attempt.sourceHash != null) {
            val latest = latestAttempt(attempt.lessonId, attempt.mode)
            if (latest?.sourceHash == attempt.sourceHash) return latest
        }
        return attempt.copy(id = addAttempt(attempt))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDraft(draft: Draft)

    @Query("SELECT source FROM drafts WHERE lessonId = :id")
    suspend fun draft(id: String): String?

    @Query("SELECT * FROM readings WHERE lessonId = :id")
    fun reading(id: String): Flow<Reading?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReading(reading: Reading)

    @Query("SELECT * FROM preferences WHERE id = 1")
    fun preferences(): Flow<StudyPreferences?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensurePreferences(preferences: StudyPreferences)

    @Query("UPDATE preferences SET showHints = :enabled WHERE id = 1")
    suspend fun updateHints(enabled: Boolean)

    @Query("UPDATE preferences SET reminders = :enabled WHERE id = 1")
    suspend fun updateReminders(enabled: Boolean)

    @Transaction
    suspend fun setHints(enabled: Boolean) {
        ensurePreferences(StudyPreferences())
        updateHints(enabled)
    }

    @Transaction
    suspend fun setReminderPreference(enabled: Boolean) {
        ensurePreferences(StudyPreferences())
        updateReminders(enabled)
    }

    @Query("DELETE FROM attempts")
    suspend fun deleteAttempts()

    @Query("DELETE FROM drafts")
    suspend fun deleteDrafts()

    @Transaction
    suspend fun clearProgress() {
        deleteAttempts()
        deleteDrafts()
    }
}

@Database(
    entities = [Attempt::class, Draft::class, StudyPreferences::class, Reading::class],
    version = 2,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attempts ADD COLUMN mode TEXT NOT NULL DEFAULT 'LEGACY'")
                db.execSQL("ALTER TABLE attempts ADD COLUMN sourceHash TEXT")
            }
        }
    }
}
