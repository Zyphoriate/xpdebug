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

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class ModuleMain extends XposedModule {

    private static final String TAG = "XposedDebug";

    private boolean toastShown = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Process: " + param.getProcessName());
        log(Log.INFO, TAG, "Framework: " + getFrameworkName() + " v" + getFrameworkVersion());
        log(Log.INFO, TAG, "API: " + getApiVersion());
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        log(Log.INFO, TAG, "Package loaded: " + param.getPackageName());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        log(Log.INFO, TAG, "Package ready: " + param.getPackageName());

        if (!param.isFirstPackage()) return;

        try {
            hookActivityLifecycle();
            hookDialogLifecycle();
            hookPopupWindow();
            log(Log.INFO, TAG, "All hooks installed for " + param.getPackageName());
        } catch (Exception e) {
            log(Log.ERROR, TAG, "Failed to install hooks", e);
        }
    }

    // ── Activity lifecycle ────────────────────────────────────────────

    private void hookActivityLifecycle() throws NoSuchMethodException {
        hook(Activity.class.getDeclaredMethod("onCreate", Bundle.class))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    Intent intent = a.getIntent();
                    log(Log.INFO, TAG, "┌──────────────────────────────────────────");
                    log(Log.INFO, TAG, "│ [Activity] onCreate");
                    log(Log.INFO, TAG, "│   Class  : " + a.getClass().getName());
                    log(Log.INFO, TAG, "│   Intent : " + (intent != null ? intent.toString() : "null"));
                    log(Log.INFO, TAG, "│   TaskId : " + a.getTaskId());
                    Bundle extras = (intent != null) ? intent.getExtras() : null;
                    if (extras != null && !extras.isEmpty()) {
                        for (String key : extras.keySet()) {
                            log(Log.INFO, TAG, "│   Extra  : " + key + " = " + extras.get(key));
                        }
                    }
                    log(Log.INFO, TAG, "└──────────────────────────────────────────");
                    showToastOnce(a);
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onStart"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    log(Log.INFO, TAG, "▶ [Activity] onStart  : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onResume"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    log(Log.INFO, TAG, "▶▶ [Activity] onResume : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onPause"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    log(Log.INFO, TAG, "⏸ [Activity] onPause  : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onStop"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    log(Log.INFO, TAG, "⏹ [Activity] onStop   : " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onDestroy"))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    log(Log.INFO, TAG, "✕ [Activity] onDestroy: " + a.getClass().getName());
                    return chain.proceed();
                });

        hook(Activity.class.getDeclaredMethod("onNewIntent", Intent.class))
                .intercept(chain -> {
                    Activity a = (Activity) chain.getThisObject();
                    Intent i = (Intent) chain.getArg(0);
                    log(Log.INFO, TAG, "↻ [Activity] onNewIntent: " + a.getClass().getName());
                    log(Log.INFO, TAG, "   New Intent: " + (i != null ? i.toString() : "null"));
                    return chain.proceed();
                });
    }

    // ── Dialog lifecycle ──────────────────────────────────────────────

    private void hookDialogLifecycle() throws NoSuchMethodException {
        hook(Dialog.class.getDeclaredMethod("show"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    log(Log.INFO, TAG, "┌──────────────────────────────────────────");
                    log(Log.INFO, TAG, "│ [Dialog] show");
                    log(Log.INFO, TAG, "│   Class  : " + d.getClass().getName());
                    if (d.getOwnerActivity() != null) {
                        log(Log.INFO, TAG, "│   Owner  : " + d.getOwnerActivity().getClass().getName());
                    }
                    if (d.getContext() != null) {
                        log(Log.INFO, TAG, "│   Context: " + d.getContext().getClass().getName());
                    }
                    log(Log.INFO, TAG, "└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(Dialog.class.getDeclaredMethod("dismiss"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    log(Log.INFO, TAG, "✕ [Dialog] dismiss: " + d.getClass().getName());
                    return chain.proceed();
                });

        hook(Dialog.class.getDeclaredMethod("hide"))
                .intercept(chain -> {
                    Dialog d = (Dialog) chain.getThisObject();
                    log(Log.INFO, TAG, "− [Dialog] hide   : " + d.getClass().getName());
                    return chain.proceed();
                });

        // AlertDialog.Builder.show() — catches build-and-show pattern
        hook(AlertDialog.Builder.class.getDeclaredMethod("show"))
                .intercept(chain -> {
                    AlertDialog d = (AlertDialog) chain.proceed();
                    log(Log.INFO, TAG, "┌──────────────────────────────────────────");
                    log(Log.INFO, TAG, "│ [AlertDialog] Builder.show()");
                    log(Log.INFO, TAG, "│   Class  : " + d.getClass().getName());
                    log(Log.INFO, TAG, "└──────────────────────────────────────────");
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
                    log(Log.INFO, TAG, "┌──────────────────────────────────────────");
                    log(Log.INFO, TAG, "│ [PopupWindow] showAtLocation");
                    log(Log.INFO, TAG, "│   Class  : " + chain.getThisObject().getClass().getName());
                    log(Log.INFO, TAG, "└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(popupWindowClass.getDeclaredMethod("showAsDropDown",
                android.view.View.class, int.class, int.class, int.class))
                .intercept(chain -> {
                    log(Log.INFO, TAG, "┌──────────────────────────────────────────");
                    log(Log.INFO, TAG, "│ [PopupWindow] showAsDropDown");
                    log(Log.INFO, TAG, "│   Class  : " + chain.getThisObject().getClass().getName());
                    log(Log.INFO, TAG, "└──────────────────────────────────────────");
                    return chain.proceed();
                });

        hook(popupWindowClass.getDeclaredMethod("dismiss"))
                .intercept(chain -> {
                    log(Log.INFO, TAG, "✕ [PopupWindow] dismiss: " + chain.getThisObject().getClass().getName());
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
