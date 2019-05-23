package com.example.shop.catalog;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.example.shop.MainActivity;
import com.example.shop.R;
import com.example.shop.app.AppSingleton;
import com.example.shop.components.CustomSnackbar;
import com.example.shop.components.CustomSwipeRefreshLayout;
import com.example.shop.components.SoundPoolPlayer;
import com.example.shop.model.BDUtilsKt;
import com.example.shop.model.KOrder;
import com.example.shop.model.OrdersModel;
import com.example.shop.mvp.BaseActivity;
import com.example.shop.mvp.BaseFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class CatalogFragment extends BaseFragment<CatalogPresenter, CatalogMvpView> implements SwipeRefreshLayout.OnRefreshListener, CatalogMvpView,
        View.OnClickListener, ICatalog, Animator.AnimatorListener {

    public static final String TAG = "tag_recycle_products";

    private CatalogAdapter adapter;
    private RecyclerView recyclerView;
    private CustomSwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyLL;
    private TextView noGoodsTV;
    private TextView totalSummTV;
    private TextView totalSummExtraTV;
    private TextView productsCountTV;
    private CoordinatorLayout clContainer;

    private LinearLayout totalLL;
    private RelativeLayout navToPayment;
    private View amountContainer;
    private static final int duration = 200;
    private int MAX_SPANS_COUNT = 4;

    @Nullable
    private Snackbar redSnackbar;

    public static String tag() { // тег для создания этого фрагмента
        return TAG;
    }

    public static CatalogFragment newInstance() {
        final CatalogFragment fragment = new CatalogFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static CatalogFragment find(final BaseActivity a) {
        return (CatalogFragment) a.getSupportFragmentManager().findFragmentByTag(TAG);
    }

    public int getCurrentCategoryId() {
        return presenter.getCurrentCategoryId();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fr_catalog_recycle;
    }

    @NonNull
    @Override
    protected CatalogPresenter getPresenter(@NonNull CatalogMvpView view) {
        return new CatalogPresenter(view, Preferences.getInstance(), OrdersModel.Companion.getInstance(),  getBaseActivity() != null && getBaseActivity().getDeviceOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, MAX_SPANS_COUNT);
    }

    @NonNull
    @Override
    protected CatalogMvpView getMVPView() {
        return this;
    }

    @NonNull
    @Override
    protected CatalogMvpView getMVPViewEmpty() {
        return new CatalogMvpViewEmpty();
    }

    @Override
    protected void setupViews(View view) {
        recyclerView = $(R.id.recyclerView);
        swipeRefreshLayout = $(R.id.refresh);
        emptyLL = $(R.id.empty_catalog);
        totalLL = $(R.id.total_container);

        noGoodsTV = $(R.id.no_goods_text);

        navToPayment = $(R.id.nav_to_payment);
        navToPayment.setOnClickListener(this);

        RelativeLayout navToBasket = $(R.id.nav_to_basket);
        navToBasket.setOnClickListener(this);

        totalSummTV = $(R.id.total_summ);
        totalSummExtraTV = $(R.id.total_summ_extra);
        productsCountTV = $(R.id.total_products);
        amountContainer = $(R.id.items_amount_container);

        this.clContainer = $(R.id.clContainer);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setChildView(recyclerView);
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_grey, R.color.blue);
        recyclerView.bringToFront();
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), MAX_SPANS_COUNT));
        recyclerView.addOnScrollListener(new SwipeRefreshLayoutToggleScrollListener());

        AppSingleton.Companion.getINSTANCE().getAnalyticSender().screen(getAnalyticName());

        new Handler().post(this::showRegistrationGreetingIf);

        if (getBaseActivity() != null && !Preferences.getInstance().isAeroMar() && !Preferences.getInstance().isMaxipos())
            LocationUtilsKt.requestAndSaveLocationIf(getBaseActivity());
    }

    private void showRegistrationGreetingIf() {
        if (getBaseActivity() != null && !Preferences.getInstance().getRegistrationTempEmail().isEmpty()) {
            getBaseActivity().showDialog(RegistrationGreetingDialog.Companion.newInstance(Preferences.getInstance().getRegistrationTempEmail()));
            Preferences.getInstance().setRegistrationTempEmail(null);
        }
    }

    @Override
    public void showBlueTopSnackbar() {
        CustomSnackbar.Companion.makeBlueTopSnackbar(this.getString(R.string.there_has_been_changes), this.getString(R.string.refresh), this.clContainer, () -> {
            onRefresh();
            return null;
        }).show();
    }

    @Override
    public boolean doesCreateContratorDialogExist() {
        if (getBaseActivity() != null) {
            Fragment fragment = getBaseActivity().getSupportFragmentManager().findFragmentByTag(BaseActivity.TAG_DIALOG);
            return fragment instanceof CreateContractorDialog;
        }
        return false;
    }

    @Override
    public void updateCatalog() {
        onRefresh();
    }

    private void showRedTopSnackbar(@NonNull String errorText) {
        this.redSnackbar = CustomSnackbar.Companion.makeRedTopSnackbar(this.getString(R.string.catalog_loading_is_interrupted) + "\n" + errorText, this.getString(R.string.go_on), this.clContainer, () -> {
            onRefresh();
            return null;
        });
        this.redSnackbar.show();
    }

    private void hideRedTopSnackbar() {
        if (this.redSnackbar != null && this.redSnackbar.isShownOrQueued())
            this.redSnackbar.dismiss();
    }

    /**
     * Устанавливает вьюшки в состояние, которое соответствует загрузке
     *
     * @param loading - true, если идёт загрузка
     * @param success - true, если загрузка завершилась успехом. Параметр игнорируется если {@param loading} - true
     */
    private void setLoadingState(boolean loading, boolean success, @Nullable String errorString) {
        if (loading) {
            if (!this.swipeRefreshLayout.isRefreshing())
                this.swipeRefreshLayout.setRefreshing(true);
            this.hideRedTopSnackbar();
        } else {
            if (this.swipeRefreshLayout.isRefreshing())
                this.swipeRefreshLayout.setRefreshing(false);
            if (success)
                this.hideRedTopSnackbar();
            else if (!Preferences.getInstance().getHaveFullLoadedCatalog())
                this.showRedTopSnackbar(errorString != null ? errorString : "Неизвестная ошибка");
        }
    }

    @Override
    public void showAddModificationsDialog(ArrayList<VariantAdapterItem> list) {
        ModificationsAddDialog.invokeModesAdd(getBaseActivity(), list);
    }


    public void showWeightAmountDialogAdd(Variant variant) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showDialog(WeightAmountDialog.newInstanceAdd(variant));
    }

    @Override
    public final void onCategorySelected(@Nullable Category category) { // будем добавлять в начало списка - так проще организовать стек
        if (category != null)
            presenter.categorySelected(category);
    }

    @Override
    public void updateTitle(String string) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.setTitle(string);
    }

    public void setGridPresentationMode() {
        adapter = new CatalogAdapter(RecyclerView.HORIZONTAL, this);
        int spacing = 10;
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(MAX_SPANS_COUNT, spacing, true));
        recyclerView.setAdapter(adapter);
        ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(new SpanSizeLookup(adapter, true, MAX_SPANS_COUNT));
    }

    public void setListPresentationMode() {
        adapter = new CatalogAdapter(RecyclerView.VERTICAL, this);
        recyclerView.setAdapter(adapter);
        ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(new SpanSizeLookup(adapter, false, MAX_SPANS_COUNT));
    }

    @Override
    public void setCatalogData(List<BaseCatalogItem> catalogList) {
        adapter.setData(catalogList);
    }

    public void showEmptyLayout() {
        if (isInRootCategory()) showEmptyCatalog();
        else showEmptyDirectory();
    }

    public void showEmptyCatalog() {
        recyclerView.setVisibility(View.GONE);
        emptyLL.setVisibility(View.VISIBLE);
        noGoodsTV.setText(R.string.no_goods);
    }

    public void showEmptyDirectory() {
        recyclerView.setVisibility(View.GONE);
        emptyLL.setVisibility(View.VISIBLE);
        noGoodsTV.setText(R.string.no_goods_directory);
    }

    public void showNonEmptyLayout() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyLL.setVisibility(View.GONE);
    }

    //4 android backbutton
    public final boolean isInRootCategory() {
        return presenter.isInRootCategory();
    }

    public final void navigateUp() {
        presenter.navigateUp();
    }

    public void enableNavUpBtn(boolean b) {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity instanceof MainActivity)
            ((MainActivity) baseActivity).setRootCatalogState(!b);

    }

    @Override
    public void showDefaultTitle() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.setTitle(getString(R.string.sale));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.nav_to_basket:
                navigate2Basket();
                break;

            case R.id.nav_to_payment:
                presenter.nav2Calc();
                break;
        }
    }

    public void navigate2Basket() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null) {
            baseActivity.finish();
            if (navigator != null)
                navigator.navigate2BasketUser();
            baseActivity.overridePendingTransition(R.anim.pull_in_from_right, R.anim.pull_out_to_right);
        }
    }

    public void navigate2Calc() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null) {
            baseActivity.finish();
            if (navigator != null)
                navigator.navigate2Calc();
            baseActivity.overridePendingTransition(R.anim.pull_in_from_right, R.anim.pull_out_to_right);
        }
    }

    public void update(KOrder order) {
        String basketItemsCount;
        basketItemsCount = (order.getTotalCountInBasket() > 99) ? "99" : String.valueOf(order.getTotalCountInBasket());

        totalSummExtraTV.setVisibility(View.GONE);
        totalSummTV.setVisibility(View.VISIBLE);
        totalSummTV.setText(BDUtilsKt.formatPrice(order.getTotalPrice()));
        productsCountTV.setText(basketItemsCount);

        playAnim();

        if (order.isEmpty()) {
            navToPayment.setEnabled(false);
            navToPayment.setAlpha(0.5f);
            amountContainer.setVisibility(View.GONE);
        } else {
            navToPayment.setEnabled(true);
            navToPayment.setAlpha(1f);
            amountContainer.setVisibility(View.VISIBLE);
            productsCountTV.setText(basketItemsCount);
        }
    }

    @Override
    public void showTotalBar(boolean isPortrait) {
        totalLL.setVisibility(isPortrait ? View.VISIBLE : View.GONE);
    }

    @Override
    public void processOversellingError() {
        Context context = getContext();
        if (context != null)
            SoundPoolPlayer.Companion.getInstance(context).playShortResource(R.raw.fail);
        showErrToast(getString(R.string.sell_in_minus_dialog_title));
    }

    @Override
    public void processSuccessScanSound() {
        Context context = getContext();
        if (context != null)
            SoundPoolPlayer.Companion.getInstance(context).playShortResource(R.raw.succ);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }
    }

    @Override
    public void onRefresh() {
        this.setLoadingState(true, false, null);
        RefreshCatalogInterface refreshCatalogInterface = (RefreshCatalogInterface) getBaseActivity();
        if (refreshCatalogInterface != null)
            refreshCatalogInterface.refreshCatalog();
    }

    @Override
    public void stopRefresh(boolean success, @Nullable String errorText) {
        this.setLoadingState(false, success, errorText);
    }

    @Override
    public void onProductDetailsSelected(Product product) {
        presenter.onSelectByLongClick(product);
        /*
        Order fakeOrder = new OrderFromTypeMapper().map(Order.TYPE_BY_USER);
        fakeOrder.add(product.getVariantSingle(), 1);
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showDialog(DetailedDialog.newInstance(0, fakeOrder, DetailedDialog.LONG_CLICK_MODE));*/
    }

    @Override
    public void showDetailedDialog() {
        BaseActivity baseActivity = getBaseActivity();
        if (baseActivity != null)
            baseActivity.showDialog(DetailedDialog.newInstance(0,  DetailedDialog.LONG_CLICK_MODE));
    }

    @Override
    public void onProductSelected(Product product) {
        presenter.productSelected(product);
    }

    private static class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        final CatalogAdapter adapter;
        final boolean isGrid;
        private final int MAX_SPANS_COUNT;

        SpanSizeLookup(final CatalogAdapter adapter, final boolean isGrid, final int MAX_SPANS_COUNT) {
            this.adapter = adapter;
            this.isGrid = isGrid;
            this.MAX_SPANS_COUNT = MAX_SPANS_COUNT;
        }

        @Override
        public final int getSpanSize(final int position) {
            if (this.isGrid) switch (this.adapter.getItemViewType(position)) {
                case BaseCatalogItem.TYPE_CATEGORY:
                    return 1;
                case BaseCatalogItem.TYPE_PRODUCT:
                    return 1;
            }
            return MAX_SPANS_COUNT;
        }
    }


    private class SwipeRefreshLayoutToggleScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (presenter.isHolderAtEnd())
                return;

            final GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
            final int visibleItemCount = manager.getChildCount();
            final int totalItemCount = manager.getItemCount();
            final int pastVisibleItems = manager.findFirstVisibleItemPosition();
            presenter.processScroll(visibleItemCount, totalItemCount, pastVisibleItems);
        }
    }

    public final void playAnim() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(totalLL, "alpha", 0.7f);
        anim.setDuration(duration);
        anim.addListener(this);
        anim.start();
    }

    @Override
    public final void onAnimationStart(final Animator animation) {
    }

    @Override
    public final void onAnimationEnd(final Animator animation) {
        ObjectAnimator.ofFloat(totalLL, "alpha", 1).setDuration(duration).start();
    }

    @Override
    public final void onAnimationCancel(final Animator animation) {
    }

    @Override
    public final void onAnimationRepeat(final Animator animation) {
    }

    public interface RefreshCatalogInterface {
        void refreshCatalog();
    }
}