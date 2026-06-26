package com.tangoplus.matviewer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [MatRecord::class],
	version = 1
)

abstract class MatDatabase : RoomDatabase() {
	abstract fun MatDao() : MatDao

	companion object {
		@Volatile
		private var INSTANCE : MatDatabase? = null
		fun getDatabase(context: Context) : MatDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					MatDatabase::class.java,
					"mat_database"
				)
					.build()
				INSTANCE = instance
				instance
			}
		}

		fun closeDatabase() {
			INSTANCE?.close()
			INSTANCE = null
		}
	}
}