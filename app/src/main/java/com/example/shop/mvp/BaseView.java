package com.example.shop.mvp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 14.11.16
 * Time: 14:42
 */
public interface BaseView extends AnalyticBaseView {

    void showWaitPrintingDialog();

    void showWaitCardProcessingDialog();

    void showWaitDialogWithText(String msg);

    void hideWaitDialog();

    void hideDialog();

    void showErrToast(@Nullable String msg);

    void showSuccToast(@NonNull String msg);

    void showWarningAlert(@Nullable String text);

    void setWaitMessage(String msg);
}
