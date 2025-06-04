package com.hjq.permissions;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2023/04/05
 *    desc   : 权限页意图处理器
 */
final class PermissionActivityIntentHandler {

    /** 存取子意图所用的 Intent Key */
    private static final String SUB_INTENT_KEY = "sub_intent_key";

    /**
     * 从父意图中获取子意图
     *
     * @param superIntent           父意图对象
     */
    private static Intent findSubIntentBySuperIntent(@NonNull Intent superIntent) {
        Intent subIntent;
        if (AndroidVersionTools.isAndroid13()) {
            subIntent = superIntent.getParcelableExtra(SUB_INTENT_KEY, Intent.class);
        } else {
            subIntent = superIntent.getParcelableExtra(SUB_INTENT_KEY);
        }
        return subIntent;
    }

    /**
     * 获取意图中最深层的子意图
     *
     * @param intent                意图对象
     */
    private static Intent findDeepIntent(@NonNull Intent intent) {
        Intent subIntent = findSubIntentBySuperIntent(intent);
        if (subIntent != null) {
            return findDeepIntent(subIntent);
        }
        return intent;
    }

    /**
     * 将子意图添加到主意图中
     *
     * @param mainIntent            主意图对象
     * @param subIntent             子意图对象
     */
    static Intent addSubIntentForMainIntent(@Nullable Intent mainIntent, @Nullable Intent subIntent) {
        if (mainIntent == null && subIntent != null) {
            return subIntent;
        }
        if (subIntent == null) {
            return mainIntent;
        }
        Intent deepSubIntent = findDeepIntent(mainIntent);
        deepSubIntent.putExtra(SUB_INTENT_KEY, subIntent);
        return mainIntent;
    }

    static boolean startActivity(@NonNull Context context, Intent intent) {
        return startActivity(new StartActivityDelegateByContext(context), intent);
    }

    static boolean startActivity(@NonNull Activity activity, Intent intent) {
        return startActivity(new StartActivityDelegateByActivity(activity), intent);
    }

    @SuppressWarnings("deprecation")
    static boolean startActivity(@NonNull Fragment fragment, Intent intent) {
        return startActivity(new StartActivityDelegateByFragmentApp(fragment), intent);
    }

    static boolean startActivity(@NonNull androidx.fragment.app.Fragment fragment, Intent intent) {
        return startActivity(new StartActivityDelegateByFragmentSupport(fragment), intent);
    }

    static boolean startActivity(@NonNull IStartActivityDelegate delegate, @NonNull Intent intent) {
        try {
            delegate.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Intent subIntent = findSubIntentBySuperIntent(intent);
            if (subIntent == null) {
                return false;
            }
            return startActivity(delegate, subIntent);
        }
    }

    static boolean startActivityForResult(@NonNull Activity activity, @NonNull Intent intent, int requestCode) {
        return startActivityForResult(new StartActivityDelegateByActivity(activity), intent, requestCode);
    }

    @SuppressWarnings("deprecation")
    static boolean startActivityForResult(@NonNull Fragment fragment, @NonNull Intent intent, int requestCode) {
        return startActivityForResult(new StartActivityDelegateByFragmentApp(fragment), intent, requestCode);
    }

    static boolean startActivityForResult(@NonNull androidx.fragment.app.Fragment fragment, @NonNull Intent intent, int requestCode) {
        return startActivityForResult(new StartActivityDelegateByFragmentSupport(fragment), intent, requestCode);
    }

    static boolean startActivityForResult(@NonNull IStartActivityDelegate delegate, @NonNull Intent intent, int requestCode) {
        try {
            delegate.startActivityForResult(intent, requestCode);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Intent subIntent = findSubIntentBySuperIntent(intent);
            if (subIntent == null) {
                return false;
            }
            return startActivityForResult(delegate, subIntent, requestCode);
        }
    }
}