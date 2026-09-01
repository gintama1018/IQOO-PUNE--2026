package com.skilllens.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities  = [SessionEntity::class, StepResultEntity::class],
    version   = 1,
    exportSchema = false,
)
abstract class SkillLensDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
