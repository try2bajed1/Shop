package com.example.shop.mvp;

import android.support.annotation.NonNull;


public abstract class BasePresenter<V> implements Presenter {

    @NonNull
    protected V view;

    public BasePresenter(@NonNull V view) {
        this.view = view;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onResume() {
        // Empty.
    }

    @Override
    public void onPause() {
        // Empty.
    }

    @Override
    public void onStop() {
        // Empty.
    }

    @Override
    public void onDestroy() {
        // Empty.
    }

    @NonNull
    public BasePresenter setView(@NonNull V v) {
        view = v;
        return this;
    }

    @NonNull
    public V getView() {
        return view;
    }
}