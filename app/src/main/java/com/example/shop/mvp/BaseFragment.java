package com.example.shop.mvp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;
import org.jetbrains.annotations.NotNull;

public abstract class BaseFragment<P extends BasePresenter, V> extends Fragment {

    @Nullable
    protected Navigator navigator;
    @NonNull
    protected P presenter;

    protected abstract int getLayoutId();

    protected abstract void setupViews(@NonNull View view);

    @NonNull
    protected abstract P getPresenter(@NonNull V view);

    @NonNull
    protected abstract V getMVPView();

    @NonNull
    protected abstract V getMVPViewEmpty();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        presenter = getPresenter(getMVPView());
        presenter.onCreate();
    }

    /**
     * ВАЖНО!!! Метод должен вызываться внутри фрагмента после вызова onViewCreated, когда getView() уже не будет нулевым
     */
    @SuppressWarnings("unchecked cast")
    public <V extends View> V $(@IdRes int res) {
        return (V) getView().findViewById(res);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        BaseActivity baseActivity = getBaseActivity();
        FragmentManager fragmentManager = getFragmentManager();
        if (baseActivity != null && fragmentManager != null)
            navigator = new Navigator(baseActivity, fragmentManager);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.navigator = null;
    }

    public static void expand(final View v) {
        v.measure(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? WindowManager.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density) * 2);
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density) * 2);
        v.startAnimation(a);
    }

    protected void setVisibilityGone(View view) {
        if (view != null)
            view.setVisibility(View.GONE);
    }

    protected void setVisibilityVisible(View view) {
        if (view != null)
            view.setVisibility(View.VISIBLE);
    }

    protected void setTextVisible(TextView tv, String str) {
        if (tv != null) {
            tv.setText(str);
            tv.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    protected BaseActivity getBaseActivity() {
        return ((BaseActivity) getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDestroy();
        presenter.view = getMVPViewEmpty();
    }

    public void showWaitPrintingDialog() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showWaitPrintingDialog();
    }

    public void showWaitCardProcessingDialog() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showWaitCardProcessingDialog();
    }

    public void showWaitDialogWithText(String msg) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showWaitDialogWithText(msg);
    }

    public void hideWaitDialog() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.hideWaitDialog();
    }

    public void hideDialog() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.hideDialog();
    }

    public void showErrToast(String msg) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showErrToast(msg);
    }

    public void showSuccToast(@NonNull String msg) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showSuccToast(msg);
    }

    public void showWarningAlert(@Nullable String text) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showWarningAlert(text);
    }

    public void setWaitMessage(String msg) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.setWaitMessage(msg);
    }

    @NotNull
    public String getAnalyticName() {
        return AnalyticScreens.Companion.getNameForClass(this.getClass());
    }
}