package net.altermundi.rn_nsd

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.*

import java.util.Collections.emptyList

class NsdPackage : ReactPackage {

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList<ViewManager<*, *>>()
    }

    override fun createNativeModules(
            reactContext: ReactApplicationContext): List<NativeModule> {
        val modules = ArrayList<NativeModule>()
        modules.add(NsdModule(reactContext))
        return modules
    }

//    override fun createJSModules(): MutableList<Class<out JavaScriptModule>> {
//        return Collections.emptyList();
//    }
}
