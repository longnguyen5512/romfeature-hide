package com.fix.romfeatures;

import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Binder;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.ArrayList;
import java.util.List;

public class RomFeatureHideHook implements IXposedHookLoadPackage {
    private static final String TAG = "RomFeatureHide";

    private static boolean isCustomRomPrefix(String name) {
        if (name == null) return false;
        return name.startsWith("org.lineageos.")
                || name.startsWith("lineageos.")
                || name.startsWith("org.omnirom.")
                || name.startsWith("org.evolution.")
                || name.startsWith("org.crdroid.")
                || name.startsWith("com.libremobileos.")
                || name.startsWith("com.android.axion.")
                || name.startsWith("io.chaldeaprjkt.")
                || name.equals("com.android.applocker")
                || name.equals("com.chiller3.bcr")
                || name.equals("com.android.frameworks.core.batterystatsviewer");
    }

    private static boolean shouldFilterCaller() {
        return (Binder.getCallingUid() % 100000) >= 10000;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName) && !"system".equals(lpparam.packageName)) return;
        Log.i(TAG, "Loaded in System Framework (" + lpparam.processName + ")");

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.server.pm.PackageManagerService",
                    lpparam.classLoader,
                    "hasSystemFeature",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[0];
                            if (shouldFilterCaller() && isCustomRomPrefix(name)) {
                                param.setResult(Boolean.FALSE);
                            }
                        }
                    });
            Log.i(TAG, "Hooked PackageManagerService.hasSystemFeature");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook hasSystemFeature: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.server.pm.PackageManagerService$IPackageManagerImpl",
                    lpparam.classLoader,
                    "getSystemAvailableFeatures",
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldFilterCaller()) return;
                            Object res = param.getResult();
                            if (!(res instanceof ParceledListSlice)) return;
                            ParceledListSlice<FeatureInfo> slice = (ParceledListSlice<FeatureInfo>) res;
                            List<FeatureInfo> orig = slice.getList();
                            if (orig == null || orig.isEmpty()) return;
                            List<FeatureInfo> filtered = null;
                            for (int i = 0; i < orig.size(); i++) {
                                FeatureInfo fi = orig.get(i);
                                if (fi != null && isCustomRomPrefix(fi.name)) {
                                    if (filtered == null) {
                                        filtered = new ArrayList<>(orig.subList(0, i));
                                    }
                                } else if (filtered != null) {
                                    filtered.add(fi);
                                }
                            }
                            if (filtered != null) {
                                param.setResult(new ParceledListSlice<>(filtered));
                            }
                        }
                    });
            Log.i(TAG, "Hooked IPackageManagerImpl.getSystemAvailableFeatures");
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook getSystemAvailableFeatures: " + t);
        }

        hookPermissions(lpparam.classLoader);
        hookPackageManagerQueries(lpparam.classLoader);
    }

    private static void hookPermissions(ClassLoader cl) {
        Class<?> permSvc = XposedHelpers.findClassIfExists(
                "com.android.server.permission.access.permission.PermissionService", cl);
        if (permSvc == null) return;

        try {
            XposedHelpers.findAndHookMethod(
                    permSvc,
                    "getPermissionInfo",
                    String.class,
                    int.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (shouldFilterCaller() && isCustomRomPrefix((String) param.args[0])) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook PermissionService.getPermissionInfo: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    permSvc,
                    "queryPermissionsByGroup",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldFilterCaller()) return;
                            Object res = param.getResult();
                            if (!(res instanceof List)) return;
                            List<PermissionInfo> orig = (List<PermissionInfo>) res;
                            List<PermissionInfo> filtered = null;
                            for (int i = 0; i < orig.size(); i++) {
                                PermissionInfo pi = orig.get(i);
                                if (pi != null && (isCustomRomPrefix(pi.name) || isCustomRomPrefix(pi.packageName))) {
                                    if (filtered == null) filtered = new ArrayList<>(orig.subList(0, i));
                                } else if (filtered != null) {
                                    filtered.add(pi);
                                }
                            }
                            if (filtered != null) param.setResult(filtered);
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook PermissionService.queryPermissionsByGroup: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    permSvc,
                    "getAllPermissionGroups",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldFilterCaller()) return;
                            Object res = param.getResult();
                            if (!(res instanceof List)) return;
                            List<PermissionGroupInfo> orig = (List<PermissionGroupInfo>) res;
                            List<PermissionGroupInfo> filtered = null;
                            for (int i = 0; i < orig.size(); i++) {
                                PermissionGroupInfo pgi = orig.get(i);
                                if (pgi != null && (isCustomRomPrefix(pgi.name) || isCustomRomPrefix(pgi.packageName))) {
                                    if (filtered == null) filtered = new ArrayList<>(orig.subList(0, i));
                                } else if (filtered != null) {
                                    filtered.add(pgi);
                                }
                            }
                            if (filtered != null) param.setResult(filtered);
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook PermissionService.getAllPermissionGroups: " + t);
        }
    }

    private static void hookPackageManagerQueries(ClassLoader cl) {
        Class<?> pmBase = XposedHelpers.findClassIfExists("com.android.server.pm.IPackageManagerBase", cl);
        if (pmBase == null) return;

        try {
            XposedHelpers.findAndHookMethod(
                    pmBase,
                    "getPackagesForUid",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldFilterCaller()) return;
                            if (param.args[0] instanceof Integer
                                    && ((Integer) param.args[0]).intValue() == Binder.getCallingUid()) {
                                return;
                            }
                            String[] pkgs = (String[]) param.getResult();
                            if (pkgs == null || pkgs.length == 0) return;
                            List<String> filtered = null;
                            for (int i = 0; i < pkgs.length; i++) {
                                if (isCustomRomPrefix(pkgs[i])) {
                                    if (filtered == null) {
                                        filtered = new ArrayList<>(pkgs.length);
                                        for (int j = 0; j < i; j++) filtered.add(pkgs[j]);
                                    }
                                } else if (filtered != null) {
                                    filtered.add(pkgs[i]);
                                }
                            }
                            if (filtered != null) {
                                param.setResult(filtered.isEmpty() ? null : filtered.toArray(new String[0]));
                            }
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook getPackagesForUid: " + t);
        }

        try {
            XposedBridge.hookAllMethods(
                    pmBase,
                    "getPackageInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldFilterCaller()) return;
                            Object res = param.getResult();
                            if (!(res instanceof PackageInfo)) return;
                            PackageInfo pi = (PackageInfo) res;
                            if ("android".equals(pi.packageName) && pi.permissions != null) {
                                List<PermissionInfo> kept = null;
                                for (int i = 0; i < pi.permissions.length; i++) {
                                    PermissionInfo p = pi.permissions[i];
                                    if (p != null && isCustomRomPrefix(p.name)) {
                                        if (kept == null) {
                                            kept = new ArrayList<>(pi.permissions.length);
                                            for (int j = 0; j < i; j++) kept.add(pi.permissions[j]);
                                        }
                                    } else if (kept != null) {
                                        kept.add(p);
                                    }
                                }
                                if (kept != null) {
                                    pi.permissions = kept.toArray(new PermissionInfo[0]);
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook getPackageInfo: " + t);
        }
    }
}
