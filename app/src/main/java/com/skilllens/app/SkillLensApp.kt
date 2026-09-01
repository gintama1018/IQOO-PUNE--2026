package com.skilllens.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * SkillLens Application class.
 *
 * Initialises app-wide dependencies:
 * - Hilt dependency injection
 * - Timber logging (debug builds only)
 */
@HiltAndroidApp
class SkillLensApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant Timber debug tree only in debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
