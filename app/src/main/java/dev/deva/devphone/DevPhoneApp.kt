package dev.deva.devphone

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.deva.devphone.line.LineManager
import kotlinx.coroutines.MainScope

class DevPhoneApp : Application() {
    val scope = MainScope()
    lateinit var line: LineManager
        private set

    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        line = LineManager(this, scope)
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityStarted(a: Activity) {
                    if (startedActivities++ == 0) line.onUiVisible(true)
                }

                override fun onActivityStopped(a: Activity) {
                    if (--startedActivities == 0) line.onUiVisible(false)
                }

                override fun onActivityCreated(
                    a: Activity,
                    b: Bundle?,
                ) = Unit

                override fun onActivityResumed(a: Activity) = Unit

                override fun onActivityPaused(a: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    a: Activity,
                    b: Bundle,
                ) = Unit

                override fun onActivityDestroyed(a: Activity) = Unit
            },
        )
    }
}

val android.content.Context.line: LineManager get() = (applicationContext as DevPhoneApp).line
