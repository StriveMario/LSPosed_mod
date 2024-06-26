/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package org.lsposed.lspd.core;

import android.app.ActivityThread;
import android.app.Application;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.util.Log;

import com.android.internal.os.ZygoteInit;

import org.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import org.lsposed.lspd.hooker.ApplicationHooker;
import org.lsposed.lspd.hooker.AttachHooker;
import org.lsposed.lspd.hooker.CrashDumpHooker;
import org.lsposed.lspd.hooker.HandleSystemServerProcessHooker;
import org.lsposed.lspd.hooker.LoadedApkCtorHooker;
import org.lsposed.lspd.hooker.LoadedApkCreateCLHooker;
import org.lsposed.lspd.hooker.OpenDexFileHooker;
import org.lsposed.lspd.impl.LSPosedContext;
import org.lsposed.lspd.impl.LSPosedHelper;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.util.Utils;

import java.util.List;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Startup {
    private static void startBootstrapHook(boolean isSystem) {
        Utils.logD("startBootstrapHook starts: isSystem = " + isSystem);
        LSPosedHelper.hookMethod(CrashDumpHooker.class, Thread.class, "dispatchUncaughtException", Throwable.class);
        if (isSystem) {
            LSPosedHelper.hookAllMethods(HandleSystemServerProcessHooker.class, ZygoteInit.class, "handleSystemServerProcess");
        } else {
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openDexFile");
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFile");
            LSPosedHelper.hookAllMethods(OpenDexFileHooker.class, DexFile.class, "openInMemoryDexFiles");
        }
        LSPosedHelper.hookConstructor(LoadedApkCtorHooker.class, LoadedApk.class,
                ActivityThread.class, ApplicationInfo.class, CompatibilityInfo.class,
                ClassLoader.class, boolean.class, boolean.class, boolean.class);
        LSPosedHelper.hookMethod(LoadedApkCreateCLHooker.class, LoadedApk.class, "createOrUpdateClassLoaderLocked", List.class);
        LSPosedHelper.hookAllMethods(AttachHooker.class, ActivityThread.class, "attach");

        //LSPosedHelper.hookMethod(ApplicationHooker.class, Application.class, "attach", Context.class);

        XposedBridge.hookAllMethods(ActivityThread.class, "performLaunchActivity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("LSPosed", "performLaunchActivity beforeHookedMethod ... ");
                Application mInitialApplication = (Application) XposedHelpers.getObjectField(param.thisObject, "mInitialApplication");
                // 获取上下文
                Context context = (Context) mInitialApplication.getApplicationContext();
                Log.i("LSPosed", "performLaunchActivity context : " + context );

                String packageName = context.getPackageName();
                String processName = packageName;
                Log.i("LSPosed", "performLaunchActivity packageName : " + packageName );
                if(packageName.contains("deepal")){
                    //深蓝的应用再注入
                    boolean isFirstPackage = packageName != null && processName != null;
                    ClassLoader classLoader = context.getClassLoader();
                    Log.i("LSPosed", "performLaunchActivity classLoader : " + classLoader);

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
        });



    }

    public static void bootstrapXposed() {
        // Initialize the Xposed framework
        try {
            startBootstrapHook(XposedInit.startsSystemServer);
            XposedInit.loadLegacyModules();
        } catch (Throwable t) {
            Utils.logE("error during Xposed initialization", t);
        }
    }

    public static void initXposed(boolean isSystem, String processName, String appDir, ILSPApplicationService service) {
        // init logger
        ApplicationServiceClient.Init(service, processName);
        XposedBridge.initXResources();
        XposedInit.startsSystemServer = isSystem;
        LSPosedContext.isSystemServer = isSystem;
        LSPosedContext.appDir = appDir;
        LSPosedContext.processName = processName;
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
    }
}
