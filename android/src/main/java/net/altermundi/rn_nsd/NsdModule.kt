/*
Copyright 2018 Nicolás Echániz <nicoechaniz@altermundi.net>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


package net.altermundi.rn_nsd;

import android.util.Log
import com.facebook.react.bridge.*

class NsdModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    internal val mNsdHelper = NsdHelper(reactContext)
    val reactContext: ReactApplicationContext = reactContext

    init{
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

