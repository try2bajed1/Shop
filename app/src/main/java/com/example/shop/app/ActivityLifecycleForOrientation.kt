package com.example.shop.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.prefs.Preferences

class ActivityLifecycleForOrientation : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity?) {}

    override fun onActivityStarted(activity: Activity?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {}

    override fun onActivityStopped(activity: Activity?) {}

    override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
        if (activity !is USBInfoActivity)
            activity?.requestedOrientation = Preferences.getInstance().orientation
    }
}