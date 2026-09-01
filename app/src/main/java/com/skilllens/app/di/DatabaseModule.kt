package com.skilllens.app.di

import android.content.Context
import androidx.room.Room
import com.skilllens.app.data.database.SessionDao
import com.skilllens.app.data.database.SkillLensDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkillLensDatabase {
        return Room.databaseBuilder(
            context,
            SkillLensDatabase::class.java,
            "skilllens_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: SkillLensDatabase): SessionDao {
        return database.sessionDao()
    }
}
