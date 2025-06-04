package com.hjq.permissions;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2025/05/20
 *    desc   : startActivity 委托 Support 包下的 Fragment 实现
 */
class StartActivityDelegateByFragmentSupport implements IStartActivityDelegate {

    @NonNull
    private final Fragment mFragment;

    StartActivityDelegateByFragmentSupport(@NonNull Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent == null) {
            return;
        }
        mFragment.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (intent == null) {
            return;
        }
        mFragment.startActivityForResult(intent, requestCode);
    }
}
