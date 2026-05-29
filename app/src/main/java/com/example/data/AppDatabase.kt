package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---- Job Tasks Query ----
    @Query("SELECT * FROM job_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<JobTask>>

    @Query("SELECT * FROM job_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): JobTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: JobTask): Long

    @Update
    suspend fun updateTask(task: JobTask)

    @Delete
    suspend fun deleteTask(task: JobTask)

    // ---- Biscateiros Query ----
    @Query("SELECT * FROM biscateiros ORDER BY id DESC")
    fun getAllBiscateiros(): Flow<List<Biscateiro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiscateiro(biscateiro: Biscateiro): Long

    @Query("SELECT COUNT(*) FROM biscateiros")
    suspend fun getBiscateirosCount(): Int

    // ---- Candidaturas Query ----
    @Query("SELECT * FROM candidaturas WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getCandidaturasForTask(taskId: Int): Flow<List<Candidatura>>

    @Query("SELECT * FROM candidaturas WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getCandidaturasForWorker(workerId: Int): Flow<List<Candidatura>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidatura(candidatura: Candidatura)

    @Update
    suspend fun updateCandidatura(candidatura: Candidatura)

    // ---- Chat Messages Query ----
    @Query("SELECT * FROM chat_messages WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getMessagesForTask(taskId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // ---- User Session Query ----
    @Query("SELECT * FROM user_sessions WHERE id = 1")
    fun getUserSessionFlow(): Flow<UserSession?>

    @Query("SELECT * FROM user_sessions WHERE id = 1")
    suspend fun getUserSession(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSession)
}

@Database(
    entities = [
        JobTask::class,
        Biscateiro::class,
        Candidatura::class,
        ChatMessage::class,
        UserSession::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biscate_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
