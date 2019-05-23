package com.example.shop.catalog;

import android.support.annotation.Nullable;
import com.example.shop.model.KOrder;

import java.util.ArrayList;
import java.util.List;

public interface CatalogMvpView extends BaseView {

    void setGridPresentationMode();

    void setListPresentationMode();

    void showEmptyLayout();

    void showNonEmptyLayout();

    void setCatalogData(List<BaseCatalogItem> catalogList);

    void navigate2Basket();

    void navigate2Calc();

    void enableNavUpBtn(boolean b);

    void showDefaultTitle();

    void updateTitle(String string);

    void stopRefresh(boolean success, @Nullable String errorText);

    void showAddModificationsDialog(ArrayList<VariantAdapterItem> list);

    void showWeightAmountDialogAdd(Variant variant);

    void update(KOrder order);

    void showTotalBar(boolean isPortrait);

    void processOversellingError();

    void processSuccessScanSound();

    void showBlueTopSnackbar();

    boolean doesCreateContratorDialogExist();

    void showDetailedDialog();

    void updateCatalog();

    void onRefresh();
}

