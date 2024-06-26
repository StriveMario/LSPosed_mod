package org.lsposed.lspd.hooker;

import android.app.ActivityThread;
import android.util.Log;

import org.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public class AttachHooker implements XposedInterface.Hooker {

//    public static void beforeHookedMethod(XposedInterface.AfterHookCallback callback) {
//
//    }
    @AfterInvocation
    public static void afterHookedMethod(XposedInterface.AfterHookCallback callback) {
        // Log.i("LSPosed", "AttachHooker afterHookedMethod ... ");

        ActivityThread actThreadObj = (ActivityThread) callback.getThisObject();

        XposedInit.loadModules(actThreadObj);
    }
}
