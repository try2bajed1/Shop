package com.example.shop.mvp;

import com.example.shop.R;


public abstract class BasePrintActivity<P extends BasePrintPresenter, V> extends BaseActivity<P, V> implements PrintView {

    @Override
    public void showPrintFailedToast() {
        showErrToast(getString(R.string.print_failed));
    }

    @Override
    public void showRetryDialog(PrintResult printResult, boolean zRetry, boolean allowSkipPrinting) {
        showRetryPrintDialog(printResult, zRetry, allowSkipPrinting);
    }

    @Override
    public void retryPrint() {
        presenter.retry();
    }

    @Override
    public void showZRequierdDialog() {
        ZDialog.invokeForRequiredZ(this);
    }

    @Override
    public void printRequierdZ() {
        presenter.printRequierdZ();
    }

    @Override
    public void showZCompleteDialog() {
        ZDialog.invokeOnZPrintComplete(this);
    }

    @Override
    public void retryZ() {
        presenter.printRequierdZ();
    }


    @Override
    public void confirmPrintNextAfterZ() {
        presenter.confirmPrintNextAfterZ();
    }


    @Override
    public void onPrintComplete() {
    }

    @Override
    public void finishOperationWithoutPrinting() {
        
    }
}