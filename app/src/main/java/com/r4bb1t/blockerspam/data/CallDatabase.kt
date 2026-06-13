package com.r4bb1t.blockerspam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BlockedCall::class, WhitelistEntry::class, BlockedMessage::class],
    version = 2,
    exportSchema = false
)
abstract class CallDatabase : RoomDatabase() {

    abstract fun callDao(): CallDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `blocked_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, `content` TEXT NOT NULL, `matchedKeyword` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: CallDatabase? = null

        fun getInstance(context: Context): CallDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CallDatabase::class.java,
                    "blocker_spam.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
}
