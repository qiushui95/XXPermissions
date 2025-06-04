package com.hjq.permissions;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2025/05/20
 *    desc   : 请求权限实现类
 */
abstract class RequestPermissionDelegateImpl implements IFragmentCallback {

    /** 请求的权限 */
    static final String REQUEST_PERMISSIONS = "request_permissions";

    /** 请求码（自动生成）*/
    static final String REQUEST_CODE = "request_code";

    /** 任务令牌 */
    @NonNull
    private final Object mTaskToken = new Object();

    /** 权限申请标记（防止系统杀死应用后重新触发请求的问题） */
    private boolean mRequestFlag;

    /** 权限请求是否已经发起 */
    private boolean mAlreadyRequest;

    /** 当前 Fragment 是否为手动解绑 */
    private boolean mManualDetach;

    /** Fragment 方法对象 */
    @NonNull
    private final IFragmentMethod<?, ?> mFragmentMethod;

    /** 权限回调对象 */
    @Nullable
    private OnPermissionFlowCallback mCallBack;

    RequestPermissionDelegateImpl(@NonNull IFragmentMethod<?, ?> fragmentMethod) {
        mFragmentMethod = fragmentMethod;
    }

    public void setRequestFlag(boolean flag) {
        mRequestFlag = flag;
    }

    void setCallback(@Nullable OnPermissionFlowCallback callback) {
        mCallBack = callback;
    }

    @Nullable
    OnPermissionFlowCallback getCallBack() {
        return mCallBack;
    }

    @Nullable
    Activity getActivity() {
        return mFragmentMethod.getActivity();
    }

    void commitDetach() {
        mManualDetach = true;
        mFragmentMethod.commitDetach();
    }

    boolean isFragmentUnavailable() {
        // 如果用户离开太久，会导致 Activity 被回收掉
        // 所以这里要判断当前 Fragment 是否有被添加到 Activity
        // 可在开发者模式中开启不保留活动来复现这个 Bug
        return !mFragmentMethod.isAdded() || mFragmentMethod.isRemoving();
    }

    void requestPermissions(@NonNull String[] permissions, int requestCode) {
        mFragmentMethod.requestPermissions(permissions, requestCode);
    }

    @Nullable
    List<String> getPermissionRequestList() {
        Bundle arguments = mFragmentMethod.getArguments();
        if (arguments == null) {
            return null;
        }
        return arguments.getStringArrayList(REQUEST_PERMISSIONS);
    }

    int getPermissionRequestCode() {
        Bundle arguments = mFragmentMethod.getArguments();
        if (arguments == null) {
            return 0;
        }
        return arguments.getInt(REQUEST_CODE);
    }

    void sendTask(@NonNull Runnable runnable, long delayMillis) {
        PermissionTaskHandler.sendTask(runnable, mTaskToken, delayMillis);
    }

    void cancelTask() {
        PermissionTaskHandler.cancelTask(mTaskToken);
    }

    IStartActivityDelegate getStartActivityDelegate() {
        return mFragmentMethod;
    }

    /**
     * 开启权限请求
     */
    abstract void startPermissionRequest(@NonNull Activity activity, @NonNull List<String> permissions, int requestCode);

    @Override
    public void onFragmentResume() {
        // 如果当前 Fragment 是通过系统重启应用触发的，则不进行权限申请
        if (!mRequestFlag) {
            mFragmentMethod.commitDetach();
            return;
        }

        // 如果在 Activity 不可见的状态下添加 Fragment 并且去申请权限会导致授权对话框显示不出来
        // 所以必须要在 Fragment 的 onResume 来申请权限，这样就可以保证应用回到前台的时候才去申请权限
        if (mAlreadyRequest) {
            return;
        }

        mAlreadyRequest = true;
        Activity activity = getActivity();
        // 检查 Activity 是不是不可用
        if (PermissionUtils.isActivityUnavailable(activity)) {
            return;
        }
        final int requestCode = getPermissionRequestCode();
        if (requestCode <= 0) {
            return;
        }
        List<String> permissions = getPermissionRequestList();
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        startPermissionRequest(activity, permissions, requestCode);
        // 锁定 Activity 屏幕方向
        ActivityOrientationControl.lockActivityOrientation(activity);
        OnPermissionFlowCallback callback = getCallBack();
        if (callback == null) {
            return;
        }
        callback.onRequestPermissionNow();
    }

    @Override
    public void onFragmentDestroy() {
        // 取消执行任务
        cancelTask();
        OnPermissionFlowCallback callBack = getCallBack();
        // 如果回调还没有置空，则证明前面没有回调权限回调完成
        if (callBack != null) {
            // 告诉外层本次权限回调有异常
            callBack.onRequestPermissionAnomaly();
            // 释放回调对象，避免内存泄漏
            setCallback(null);
        }
        if (mManualDetach) {
            return;
        }
        Activity activity = getActivity();
        // 检查 Activity 是不是不可用
        if (PermissionUtils.isActivityUnavailable(activity)) {
            return;
        }
        // 如果不是手动解绑绑定，则证明是系统解除绑定，这里需要恢复 Activity 屏幕方向
        // 如果是手动解除绑定，则会在所有的权限都申请完了之后恢复 Activity 屏幕方向
        ActivityOrientationControl.unlockActivityOrientation(activity);
    }
}