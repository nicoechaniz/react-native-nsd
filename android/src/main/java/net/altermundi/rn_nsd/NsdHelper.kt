/*
Initially translated from java to kotlin by Android Studio from this source
https://android.googlesource.com/platform/development/+/master/samples/training/NsdChat/src/com/example/android/nsdchat/NsdHelper.java

 */
package net.altermundi.rn_nsd;

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


class NsdHelper(internal var reactContext: ReactApplicationContext) {
    internal var mContext: Context = reactContext.baseContext
    internal var mNsdManager: NsdManager
    internal var mResolveListener: NsdManager.ResolveListener? = null
    internal var mDiscoveryListener: NsdManager.DiscoveryListener? = null
    internal var mRegistrationListener: NsdManager.RegistrationListener? = null
    var chosenServiceInfo: NsdServiceInfo? = null

    internal val mANDROID_ID: String = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)
    internal val mBaseServiceName = "elrepo.io"
    internal var mServiceName = "$mBaseServiceName at $mANDROID_ID"
//    internal set // from Java -> Kotlin translation // unknown use

    init {
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun initializeNsd() {
        initializeResolveListener()
        //mNsdManager.init(mContext.getMainLooper(), this);
    }

    fun initializeDiscoveryListener() {
        mDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success $service")
                if (service.serviceType != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
                } else if (service.serviceName == mServiceName) {
                    Log.d(TAG, "Same machine: $mServiceName")
                } else if (service.serviceName.contains(mBaseServiceName)) {
                    mNsdManager.resolveService(service, mResolveListener)
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

    fun initializeResolveListener() {
        mResolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. $serviceInfo")
                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }
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
        }
    }

    fun initializeRegistrationListener() {
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
        serviceInfo.serviceType = SERVICE_TYPE
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener)
    }

    fun discoverServices() {
        stopDiscovery()  // Cancel any existing discovery request
        initializeDiscoveryListener()
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
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
            } finally {
            }
            mRegistrationListener = null
        }
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
    }

    companion object {
        val SERVICE_TYPE = "_repo._tcp."
        val TAG = "ReactNative NsdHelper"
    }
}
