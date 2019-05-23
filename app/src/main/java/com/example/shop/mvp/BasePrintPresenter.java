package com.example.shop.mvp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.example.shop.utils.RxBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import java.util.prefs.Preferences;



public abstract class BasePrintPresenter<V extends PrintView> extends BasePresenter<V> {

    public BasePrintPresenter(@NonNull V view) {
        super(view);
    }

    @Nullable
    private Disposable rxBusDisposable;

    @Nullable
    protected AbsPrintCase currPrintCase;

    protected abstract void printPresenterCompleteHandler(WrapPrintData wrapPrintData);


    protected abstract void processRxBusEvent(Object o);

    @Override
    public void onCreate() {
        subscribeOnRxBus();
    }

    @Override
    public void onDestroy() {
        if (rxBusDisposable != null)
            rxBusDisposable.dispose();
    }

    private void subscribeOnRxBus() {
        rxBusDisposable = RxBus.instanceOf().getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processRxBusEvent);
    }

    public void printRequierdZ() {
        view.showWaitPrintingDialog();
        Disposable disposable = new ZReportPrintCase(true).getObs()
                .subscribe(this::finishPrintCommon, this::processError);
    }

    protected void processError(Throwable throwable) {
        throwable.printStackTrace();
        if (throwable instanceof PrintException) {
            finishPrintCommon(((PrintException) throwable).getWrapPrintData());
        } else if (throwable instanceof NoPrinterAttachedException) {
            view.hideWaitDialog();
            if (Preferences.getInstance().isStrictPrintMode())
                showRetryDialog(new PrintResult(getClass().getName(), PrintError.PRINTER_OFF, null, null,null, "", "",false), false, true);
            else
                view.showWarningAlert("Принтер не привязан.");
        } else if (throwable instanceof MaxipostPrintingException) {
            view.hideWaitDialog();
            view.showWarningAlert("Чек был добавлен в очередь, но не был распечатан");

        } else if (throwable instanceof FailToSaveOnRemoteException) {
            view.hideWaitDialog();
            view.showWarningAlert("Чек не был добавлен в очередь на печать, попробуйте позже");
        } else {
            showErrToast(PrintError.UNKNOWN); //сюда действительно попадаем при неизвестной ошибке
            if (Preferences.getInstance().isStrictPrintMode())
                showRetryDialog(new PrintResult(getClass().getName(), PrintError.UNKNOWN, null, null, null, "", "",false), false, true);
        }
    }

    protected void finishPrintCommon(WrapPrintData pair) {
        switch (pair.getResult()) {
            case OK:
                okHandler(pair);
                break;
            case RETRY:
            case ERR_TOAST:
                showRetryDialog(pair.getPrintResult(), pair.getType() == Z_REPORT,
                        Preferences.getInstance().isStrictPrintMode() || pair.paidByCard());
                break;
            case Z_REQUIRED:
                showZRequierdDialog();
                break;
        }
    }

    private void okHandler(WrapPrintData pair) {
        if (pair.getType() == Z_REPORT)
            //печать z-отчета во всех презентерах одинакова.
            onZPrintComplete(pair);
        else
            printPresenterCompleteHandler(pair); //специфику каждого кейса разруливаю в соотв. презентерах
    }

    public void printZ() {
        view.showWaitPrintingDialog();

        if (Preferences.getInstance().isAeroMar()) {
            //при закрытии смены на аэромаре выходим из аккаунта
            Disposable disposable = new ZReportPrintCase(false).getObs()
                    .subscribe(this::finishPrintCommon,
                            t -> {
                                showErrToast(PrintError.UNKNOWN);
                                aeromarZReportFailed();
                            });
        } else {
            Disposable disposable = new ZReportPrintCase(false).getObs().subscribe(this::finishPrintCommon, this::processError);
        }
    }

    protected void aeromarZReportFailed() {

    }

    private void showRetryDialog(PrintResult printResult, boolean zRetry, boolean allowSkipPrinting) {
        view.hideWaitDialog();
        view.showRetryDialog(printResult, zRetry, allowSkipPrinting);
        view.showPrintFailedToast();
    }

    private void onZPrintComplete(WrapPrintData pair) {
        if (pair.hasTail())
            view.showZCompleteDialog();
        else
            onZCompleteWithoutTail();
    }

    private void onZCompleteWithoutTail() {
        //!!!оверрайдится в  подклассах
        onPrintComplete();
    }

    private void showZRequierdDialog() {
        view.hideWaitDialog();
        view.showZRequierdDialog();
    }

    public void confirmPrintNextAfterZ() {
        view.showWaitPrintingDialog();
        invokeCurrentPrintCase();
    }

    public void retry() {
        view.showWaitPrintingDialog();
        invokeCurrentPrintCase();
    }

    private void invokeCurrentPrintCase() {
        if (currPrintCase != null) {
            Disposable disposable = currPrintCase.getObs()
                    .subscribe(this::finishPrintCommon, this::processError);
        }
    }

    //по окончанию успешной печати нужно всегда прятать прогрессбар и показывать тост
    //todo: при необходимости добавить для каждого типа печати свой текст в конце
    protected void onPrintComplete() {
        view.hideWaitDialog();
        view.onPrintComplete();
    }

    public void showErrToast(PrintError code) {
        view.hideWaitDialog();
        view.showErrToast(code.getMessage());
    }

    public void showErrToast(@NonNull String error) {
        view.hideWaitDialog();
        view.showErrToast(error);
    }
}