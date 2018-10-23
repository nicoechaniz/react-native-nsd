package net.altermundi.rn_nsd;

import android.util.Log
import com.facebook.react.bridge.*

class NsdModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    internal val mNsdHelper = NsdHelper(reactContext);
    val reactContext: ReactApplicationContext = reactContext

    init{
        mNsdHelper.initializeNsd()
        /* forward resume, pause, destroy to controller */
        reactContext.addLifecycleEventListener(object : LifecycleEventListener {
            override fun onHostResume() {
                Log.d(TAG, "host resumed")
            }

            override fun onHostPause() {
                Log.d(TAG, "host paused")
            }

            override fun onHostDestroy() {
                mNsdHelper.tearDown()
                Log.d(TAG, "host destroyed")
            }
        })
    }

    override fun getName(): String {
        return "NSD"
    }

    @ReactMethod
    fun register(port: Int) {
        mNsdHelper.registerService(port)
    }

    @ReactMethod
    fun unregister() {
        //TODO: close the server socket when service is unregistered
        mNsdHelper.tearDown()
    }

    @ReactMethod
    fun discover() {
        mNsdHelper.discoverServices()
    }

    @ReactMethod
    fun stopDiscovery() {
        mNsdHelper.stopDiscovery()
    }

    companion object {
        // We set this tag to catch the module log output with react-native log-android
        val TAG = "ReactNative NDS"
    }
}

