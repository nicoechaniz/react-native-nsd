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

/*
Initially based on code translated from java to kotlin by Android Studio from this source:
https://android.googlesource.com/platform/development/+/master/samples/training/NsdChat/src/com/example/android/nsdchat/NsdHelper.java
 */

package net.altermundi.rn_nsd

import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.net.nsd.NsdManager
import android.provider.Settings
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.content.pm.PackageManager
import java.lang.Exception
import java.lang.Thread.sleep
import java.util.*

class NsdHelper(internal val reactContext: ReactApplicationContext) {
    internal var mContext: Context = reactContext.baseContext
    internal var mNsdManager: NsdManager
    internal var mDiscoveryListener: NsdManager.DiscoveryListener? = null
    internal var mRegistrationListener: NsdManager.RegistrationListener? = null
    var chosenServiceInfo: NsdServiceInfo? = null

    internal val mANDROID_ID: String = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)

    internal var mServiceType: String
    internal var mBaseServiceName: String
    internal var mServiceName: String
    internal var mResolutionQueue = ResolutionQueue()

    init {
        // Try to read the service name and type from the App's manifest
        mBaseServiceName = readMetadata("nsdServiceName") ?: "Undefined Service"
        mServiceType = readMetadata("nsdServiceType") ?: "_undefined._tcp."
        mServiceName = "$mBaseServiceName at $mANDROID_ID"
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private fun readMetadata(key: String): String? {
        Log.d(TAG, "Reading metadata for $key")
        var value: String? = null
        val ai = reactContext.applicationInfo
        try {
            value = ai.metaData.getString(key)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.message)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.message)
        }
        return(value)
    }

    inner class ResolutionQueue {
        // NsdManager.resolveService fails when resolutions are processed concurrently.
        // this queue, while not pretty, gets the job done to process resolutions sequentially
        var queue = ArrayDeque<NsdServiceInfo>()
        var resolutionInProgress: Boolean = false

        fun add (serviceInfo: NsdServiceInfo){
            queue.addLast(serviceInfo)
            Log.e(TAG, "Current Queue: $queue")
        }

        fun resolveNext(){
            while (resolutionInProgress == true){
                sleep(1000)
                Log.d(TAG, "resolution still in progress...")
            }
            val service = queue.pollFirst()
            if (service != null) {
                Log.d(TAG, "Resolving service: $service ")
                val resolveListener = NsdResolveListener()
                resolutionInProgress = true
                mNsdManager.resolveService(service, resolveListener)
            }
        }
    }

    inner class NsdResolveListener: NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed with error code: $errorCode for $serviceInfo")
            mResolutionQueue.resolutionInProgress = false
            mResolutionQueue.resolveNext()
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")
            if (serviceInfo.serviceName == mServiceName) {
                Log.d(TAG, "Same IP.")
            } else {
                chosenServiceInfo = serviceInfo
                val port: Int = serviceInfo.port
                val host: String = serviceInfo.host.toString().replace("/", "")
                val name: String = serviceInfo.serviceName

                var params: WritableMap = Arguments.createMap()
                params.putInt("port", port)
                params.putString("host", host)
                params.putString("name", name)
                sendEvent(reactContext, "serviceResolved", params)
            }
            mResolutionQueue.resolutionInProgress = false
            // each resolution calls for the resolution of the next service in the queue
            mResolutionQueue.resolveNext()
        }
    }

    private fun initializeDiscoveryListener() {
        mDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success $service")
                if (service.serviceType != mServiceType) {
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
                } else if (service.serviceName == mServiceName) {
                    Log.d(TAG, "Same machine: $mServiceName")
                } else if (service.serviceName.contains(mBaseServiceName)) {
                    // add service to the resolution queue and start resolution if not in progress
                    mResolutionQueue.add(service)
                    if (mResolutionQueue.resolutionInProgress == false) {
                        mResolutionQueue.resolveNext()
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "service lost$service")
                if (chosenServiceInfo == service) {
                    chosenServiceInfo = null
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
            }
        }
    }

    private fun initializeRegistrationListener() {
        mRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                mServiceName = NsdServiceInfo.serviceName
                Log.d(TAG, "Service registered: $mServiceName")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.d(TAG, "Service registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: " + arg0.serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d(TAG, "Service unregistration failed: $errorCode")
            }
        }
    }

    fun registerService(port: Int) {
        tearDown()  // Cancel any previous registration request
        initializeRegistrationListener()
        val serviceInfo = NsdServiceInfo()
        serviceInfo.port = port
        serviceInfo.serviceName = mServiceName
        serviceInfo.serviceType = mServiceType
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener)
    }

    fun discoverServices() {
        stopDiscovery()  // Cancel any existing discovery request
        initializeDiscoveryListener()
        mNsdManager.discoverServices(
                mServiceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
    }

    fun stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } finally {
            }
            mDiscoveryListener = null
        }
    }

    fun tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister service.")
            }
            mRegistrationListener = null
        }
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
    }

    companion object {
        val TAG = "ReactNative NsdHelper"
    }
}
