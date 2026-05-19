package io.github.zypho.xpdebug;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class ModuleMain extends XposedModule {

    private static final String TAG = "XposedDebug";

    private boolean toastShown = false;

    // Log to both Xposed framework + adb logcat
    private void debug(String msg) {
        log(Log.INFO, TAG, msg);
        Log.i(TAG, msg);
    }

    private void debug(String msg, Throwable tr) {
        log(Log.ERROR, TAG, msg, tr);
        Log.e(TAG, msg, tr);
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        debug("Process: " + param.getProcessName());
        debug("Framework: " + getFrameworkName() + " v" + getFrameworkVersion());
        debug("API: " + getApiVersion());
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        debug("Package loaded: " + param.getPackageName());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        debug("Package ready: " + param.getPackageName());

        if (!param.isFirstPackage()) return;

        try {
            hookActivityLifecycle();
            hookDialogLifecycle();
            hookPopupWindow();
            debug("All hooks installed for " + param.getPackageName());
        } catch (Exception e) {
            debug("Failed to install hooks", e);
        }
    }

    // ── Activity lifecycle ────────────────────────────────────────────

    private void hookActivityLifecycle() throws NoSuchMethodException {
        hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    Intent intent = a.getIntent();
                    debug("┌──────────────────────────────────────────");
                    debug("│ [Activity] onCreate");
                    debug("│   Class  : " + a.getClass().getName());
                    debug("│   Intent : " + (intent != null ? intent.toString() : "null"));
                    debug("│   TaskId : " + a.getTaskId());
                    Bundle extras = (intent != null) ? intent.getExtras() : null;
                    if (extras != null && !extras.isEmpty()) {
                        for (String key : extras.keySet()) {
                            debug("│   Extra  : " + key + " = " + extras.get(key));
                        }
                    }
                    debug("└──────────────────────────────────────────");
                    showToastOnce(a);
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onStart"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    debug("▶ [Activity] onStart  : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onResume"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    debug("▶▶ [Activity] onResume : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onPause"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    debug("⏸ [Activity] onPause  : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onStop"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    debug("⏹ [Activity] onStop   : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onDestroy"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    debug("✕ [Activity] onDestroy: " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onNewIntent", Intent.class))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    Intent i = (Intent) chain.getArg(0);
                    debug("↻ [Activity] onNewIntent: " + a.getClass().getName());
                    debug("   New Intent: " + (i != null ? i.toString() : "null"));
                    return chain.proceed();
                });
    }

    // ── Dialog lifecycle ──────────────────────────────────────────────

    private void hookDialogLifecycle() throws NoSuchMethodException {
        hook(Dialog.class.getDeclaredMethod("show"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    debug("┌──────────────────────────────────────────");
                    debug("│ [Dialog] show");
                    debug("│   Class  : " + d.getClass().getName());
                    if (d.getOwnerActivity() != null) {
                        debug("│   Owner  : " + d.getOwnerActivity().getClass().getName());
                    }
                    if (d.getContext() != null) {
                        debug("│   Context: " + d.getContext().getClass().getName());
                    }
                    debug("└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(Dialog.class.getDeclaredMethod("dismiss"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    debug("✕ [Dialog] dismiss: " + d.getClass().getName());
                    return chain.proceed();
                });

        hook(Dialog.class.getDeclaredMethod("hide"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    debug("− [Dialog] hide   : " + d.getClass().getName());
                    return chain.proceed();
                });

        hook(AlertDialog.Builder.class.getDeclaredMethod("show"))
                .intercept(chain -> {
                    AlertDialog d = (AlertDialog) chain.proceed();
                    debug("┌──────────────────────────────────────────");
                    debug("│ [AlertDialog] Builder.show()");
                    debug("│   Class  : " + d.getClass().getName());
                    debug("└──────────────────────────────────────────");
                    return d;
                });
    }

    // ── PopupWindow ───────────────────────────────────────────────────

    private void hookPopupWindow() throws NoSuchMethodException {
        Class<?> popupWindowClass;
        try {
            popupWindowClass = Class.forName("android.widget.PopupWindow");
        } catch (ClassNotFoundException e) {
            return;
        }

        hook(popupWindowClass.getDeclaredMethod("showAtLocation",
                android.view.View.class, int.class, int.class, int.class))
                .intercept(chain -> {
                    debug("┌──────────────────────────────────────────");
                    debug("│ [PopupWindow] showAtLocation");
                    debug("│   Class  : " + chain.getThisObject().getClass().getName());
                    debug("└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(popupWindowClass.getDeclaredMethod("showAsDropDown",
                android.view.View.class, int.class, int.class, int.class))
                .intercept(chain -> {
                    debug("┌──────────────────────────────────────────");
                    debug("│ [PopupWindow] showAsDropDown");
                    debug("│   Class  : " + chain.getThisObject().getClass().getName());
                    debug("└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(popupWindowClass.getDeclaredMethod("dismiss"))
                .intercept(chain -> {
                    debug("✕ [PopupWindow] dismiss: " + chain.getThisObject().getClass().getName());
                    return chain.proceed();
                });
    }

    // ── Toast ─────────────────────────────────────────────────────────

    private void showToastOnce(Activity activity) {
        if (toastShown) return;
        toastShown = true;
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(activity, "Xposed Debug Hook OK", Toast.LENGTH_LONG).show());
    }
}
