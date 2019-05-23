package com.example.shop.mvp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.*;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.example.shop.R;
import com.example.shop.components.toasty.Toasty;
import com.example.shop.rest.RetrofitUtilsKt;
import com.example.shop.utils.RxBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.prefs.Preferences;

public abstract class BaseActivity<P extends BasePresenter, V> extends AppCompatActivity {

    public static final String TAG_WAIT = "waitDialog";
    public static final String TAG_DIALOG = "dialog";
    public static final String TAG_SIMPLE_ALERT = "simple_alert";
    
    @Nullable
    private Disposable accessErrorRxBusDisposable;
    @Nullable
    protected FragmentManager fm;
    @NonNull
    protected Navigator navigator;
    @NonNull
    protected P presenter;

    @NonNull
    protected abstract P getPresenter(@NonNull V view);

    @NonNull
    protected abstract V getMVPView();

    @NonNull
    protected abstract V getMVPViewEmpty();


    @SuppressWarnings("unchecked cast")
    public <V extends View> V $(@IdRes int res) {
        return (V) findViewById(res);
    }

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        setContentView(getLayoutId());
        navigator = new Navigator(this, getSupportFragmentManager());
        this.fm = getSupportFragmentManager();
        setupViews();
        presenter = getPresenter(getMVPView());
        presenter.onCreate();
    }

    protected abstract int getLayoutId();

    protected abstract void setupViews();


    @Override
    protected void onResume() {
        super.onResume();
        setNonHidingSystemBar();
        presenter.onResume();
        this.subscribeOnAccessErrorRxBus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
        this.unsubscribeOnAccessErrorRxBus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    protected void onDestroy() {
        this.fm = null;
        presenter.view = getMVPViewEmpty();
        presenter.onDestroy();
        super.onDestroy();
    }

    public void setTitle(String s) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(s);
    }

    public void showSuccToast(@NonNull String msg) {
        Toasty.success(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void showWaitPrintingDialog() {
        showWaitDialog(WaitDialog.newInstancePrint());
    }
    public void showWaitPrintingEODialog() {
        showWaitDialog(WaitDialog.newInstancePrintEO());
    }

    public void showWaitCardProcessingDialog() {
        showWaitDialog(WaitDialog.newInstanceCard());
    }

    public void showWaitDialogWithText(String msg) {
        showWaitDialog(WaitDialog.newInstanceCustomMsg(msg));
    }



    public void showErrToast(@Nullable String msg) {
        if (msg == null)
            Toasty.error(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        else
            Toasty.error(this, msg, Toast.LENGTH_LONG).show();
    }

    @Nullable
    public final Fragment getFragmentByTag(final String tag) {
        return this.fm != null ? this.fm.findFragmentByTag(tag) : null;
    }

    @Nullable
    public final Fragment getFragmentById(final int id) {
        return this.fm != null ? this.fm.findFragmentById(id) : null;
    }

    public int getDeviceOrientation() {
        return Preferences.getInstance().getOrientation();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Preferences.getInstance().isMaxipos())
            return;

        int currentApiVersion = Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void setNonHidingSystemBar() {
        if (Preferences.getInstance().isMaxipos())
            return;

//        if (SET_ACTIVITY_FULLSCREEN)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // This work only for android 4.4+
        int currentApiVersion = Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }
    }

    /**
     * Подписка на эвенты {@link AccessError} для контроля доступа
     */
    private void subscribeOnAccessErrorRxBus() {
        this.accessErrorRxBusDisposable = RxBus.instanceOf().getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(object -> {
                    if (object instanceof AccessError) {
                        switch (((AccessError) object).getException().getMessage()) {
                            case RetrofitUtilsKt.BAD_TOKEN_NOT_PAID_EXCEPTION:
                                this.go2BecauseOfNotPaid();
                                break;
                            case RetrofitUtilsKt.BAD_TOKEN_CASHBOX_IS_OFF_OR_ANOTHER_DEVICE_IS_USED_EXCEPTION:
                                this.go2BecauseOfBadToken();
                                break;
                            case RetrofitUtilsKt.CASHIER_DOES_NOT_EXISTS_EXCEPTION:
                                this.go2BecauseOfBadCashier();
                                break;
                            case RetrofitUtilsKt.ACCESS_ERROR_DURING_SHIFT_CLOSING_EXCEPTION:
                                this.go2BecauseOfBadToken();
                                break;
                        }
                    }
                }, exception -> {
                });
    }

    /**
     * Перенаправляет к оплате аккаунта, потому что срок оплаты истёк
     * В случае, если мы и так находимся на странице оплаты аккаунта, не перенаправляет
     */
    private void go2BecauseOfNotPaid() {
        //Проверяем, требуется ли перенаправление
        if (!ExpiredMessageActivity.class.equals(this.getClass())) {
            //Отписываемся от эвентов, чтобы больше не получать их
            this.unsubscribeOnAccessErrorRxBus();
            //Пытаемся закрыть все активити
            ActivityCompat.finishAffinity(this);
            //Запускаем активити для оплаты аккаунта
            this.navigator.navigate2Expired();
            //Выводим ошибку
            String expMsg = Utils.getDateString_ddMMyyyy(Preferences.getInstance().getPayTillAsDate().getTime());
            Toasty.error(this, String.format(getString(R.string.expired_msg), expMsg), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Перенаправляет к вводу регистрационного кода, потому что протух токен по какой-то неизвестной человечеству причине
     * В случае, если мы и так находимся на странице регистрации или ввода регистрационного кода, не перенаправляет
     */
    private void go2BecauseOfBadToken() {
        //Проверяем, требуется ли перенаправление
        List<Fragment> fragmentList = this.getSupportFragmentManager().getFragments();
        Fragment fragment = fragmentList.size() != 0 ? fragmentList.get(fragmentList.size() - 1) : null;
        if (!(LoginActivity.class.equals(this.getClass()) &&
                (fragment instanceof GetStartedFragment || fragment instanceof EnterBackOfficeCodeFragment))) {
            //Отписываемся от эвентов, чтобы больше не получать их
            this.unsubscribeOnAccessErrorRxBus();
            //Выходим из аккаунта
            new ExitAccountUseCase(Preferences.getInstance()).execute();
            //Пытаемся закрыть все активити
            ActivityCompat.finishAffinity(this);
            //Запускаем активити для ввода регистрационного кода
            startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                    .putExtra(LoginActivity.EXTRA_MODE, LoginPresenter.StartMode.CLEAR_ACCOUNT_MODE.toValue())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            //Выводим ошибку
            Toasty.error(this, getString(R.string.another_device_detected), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Перенаправляет к вводу пина кассира, потому что кассир удалён
     * В случае, если мы и так находимся на странице входа с любым фрагментом
     */
    private void go2BecauseOfBadCashier() {
        //Проверяем, требуется ли перенаправление
        List<Fragment> fragmentList = this.getSupportFragmentManager().getFragments();
        if (!LoginActivity.class.equals(this.getClass()) || fragmentList.isEmpty()) {
            //Отписываемся от эвентов, чтобы больше не получать их
            this.unsubscribeOnAccessErrorRxBus();
            //Пытаемся закрыть все активити
            ActivityCompat.finishAffinity(this);
            //Запускаем активити для ввода пинкода
            this.navigator.navigate2LoginChangeCashier();
            //Выводим ошибку
            Toasty.error(this, getString(R.string.cashier_has_been_removed), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Отписка от эвентов {@link AccessError} для контроля доступа
     */
    private void unsubscribeOnAccessErrorRxBus() {
        if (this.accessErrorRxBusDisposable != null)
            this.accessErrorRxBusDisposable.dispose();
    }


    public void showSimpleAlert(String headerStr, @Nullable String msg, @Nullable String tag) {
        showSimpleAlertDialog(SimpleTextDialog.Companion.newInstance(headerStr, msg != null ? msg : "", "", R.string.ok, R.string.empty), tag);
    }

    //для рендера хтмл в сообщении
    public void showSimpleAlert(String headerStr, CharSequence msg) {
        showSimpleAlertDialog(SimpleTextDialog.Companion.newInstance(headerStr, "", msg, R.string.ok, R.string.empty), null);
    }

    public void showWarningAlert(@Nullable String text, @NonNull String tag) {
        showSimpleAlert(getString(R.string.warning), text, tag);
    }

    public void showWarningAlert(@Nullable String text) {
        showSimpleAlert(getString(R.string.warning), text, null);
    }

    //этот диалог для событий от железок, которые могут дублироваться и тут не надо показывать
    private void showSimpleAlertDialog(@NonNull DialogFragment dialog, @Nullable String tag) {
        String tagForDialog = tag != null ? tag : TAG_SIMPLE_ALERT;
        if (this.getFragmentByTag(tagForDialog) != null)
            return;

        if (this.fm != null) {
            FragmentTransaction transactionFragment = fm.beginTransaction();
            transactionFragment.add(dialog, tagForDialog).commitAllowingStateLoss();
        }
        //fm.executePendingTransactions();
    }

    public void showDialog(final DialogFragment dialog) {
        showDialog(dialog, TAG_DIALOG);
    }

    public void showDialog(final DialogFragment dialog, String tag) {
        if (this.fm != null) {
            FragmentTransaction transactionFragment = fm.beginTransaction();
            transactionFragment.add(dialog, tag).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
    }

    public void hideDialog() {
        hideDialog(TAG_DIALOG);
    }

    public void hideDialog(String tag) {
        final Fragment taggedFragment = this.getFragmentByTag(tag);
        if (taggedFragment != null && this.fm != null) {
            fm.beginTransaction().remove(taggedFragment).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
    }

    public final void showWaitDialog(final DialogFragment dialog) {
        if (getFragmentByTag(TAG_WAIT) != null)
            return;

        if (this.fm != null) {
            FragmentTransaction transactionFragment = fm.beginTransaction();
            transactionFragment.add(dialog, TAG_WAIT).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
    }

    public void hideWaitDialog() {
        final Fragment taggedFragment = this.getFragmentByTag(TAG_WAIT); // коряво что всем диалогам один и тот же таг
        if (taggedFragment != null && this.fm != null) {
            fm.beginTransaction().remove(taggedFragment).commitAllowingStateLoss();
            // bachin: перенес камент из 'ProgressDialogFragment'
            // тк колбэк от pay-me устройства приходит дважды
            //  поэотму надо проверять диалог на наличие, причем именно с fm.executePendingTransactions();
            fm.executePendingTransactions();
        }
    }

    public void setWaitMessage(String msg) {
        Fragment fragment = this.getFragmentByTag(TAG_WAIT);
        if (fragment instanceof WaitDialog)
            ((WaitDialog) fragment).setCardPaymentMsg(msg);
    }

    public final void showRetryPrintDialog(PrintResult printResult, boolean zRetry, boolean allowSkipPrinting) {
        if (zRetry)
            RetryPrintDialog.invokeForZ(this, printResult.buildMessage(), allowSkipPrinting);
        else
            RetryPrintDialog.invokeForCommon(this, printResult.buildMessage(), allowSkipPrinting);
    }

    public final void addFullscreenFragment(final Fragment fragment, final String tag) {
        if (this.fm != null) {
            this.fm.beginTransaction().replace(R.id.navigation_fragments_container, fragment, tag).commitAllowingStateLoss();
            this.fm.executePendingTransactions();
        }
    }

    @NotNull
    public String getAnalyticName() {
        return AnalyticScreens.Companion.getNameForClass(this.getClass());
    }
}