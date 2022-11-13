package pink.nora.pitchup

import android.app.Application
import timber.log.Timber

class PitchupApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}