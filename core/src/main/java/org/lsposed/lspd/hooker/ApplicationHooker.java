package org.lsposed.lspd.hooker;

import android.app.ActivityThread;
import android.content.Context;
import android.util.Log;

import org.lsposed.lspd.util.Hookers;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public class ApplicationHooker implements XposedInterface.Hooker {

    //    public static void beforeHookedMethod(XposedInterface.AfterHookCallback callback) {
//
//    }
    @BeforeInvocation
    public static void beforeHookedMethod(XposedInterface.BeforeHookCallback callback) {
        Log.i("LSPosed", "ApplicationHooker beforeHookedMethod ... ");

        // 获取上下文
        Context context = (Context) callback.getArgs()[0];
        Log.i("LSPosed", "ApplicationHooker context : " + context );


        String packageName = context.getPackageName();
        String processName = packageName;
        if(packageName.contains("deepal")){
            //深蓝的应用再注入
            boolean isFirstPackage = packageName != null && processName != null;
            ClassLoader classLoader = context.getClassLoader();
            Log.i("LSPosed", "ApplicationHooker classLoader : " + classLoader);

            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                    XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = packageName;
            lpparam.processName = processName;
            lpparam.classLoader = classLoader;
            lpparam.appInfo = null;
            lpparam.isFirstApplication = isFirstPackage;
            Log.i("LSPosed", "Call handleLoadedPackage: packageName=" + lpparam.packageName + " processName=" + lpparam.processName + " isFirstPackage=" + isFirstPackage + " classLoader=" + lpparam.classLoader + " appInfo=" + lpparam.appInfo);
            XC_LoadPackage.callAll(lpparam);
        }

    }
}
