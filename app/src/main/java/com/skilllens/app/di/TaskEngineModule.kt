package com.skilllens.app.di

import com.skilllens.app.taskengine.StateMachine
import com.skilllens.app.taskengine.Validator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskEngineModule {

    @Provides
    @Singleton
    fun provideValidator(): Validator = Validator()

    @Provides
    @Singleton
    fun provideStateMachine(validator: Validator): StateMachine =
        StateMachine(validator)
}
