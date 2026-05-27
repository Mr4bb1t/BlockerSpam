package com.r4bb1t.blockerspam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BlockedCall::class, WhitelistEntry::class],
    version = 1,
    exportSchema = false
)
abstract class CallDatabase : RoomDatabase() {

    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: CallDatabase? = null

        fun getInstance(context: Context): CallDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CallDatabase::class.java,
                    "blocker_spam.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
