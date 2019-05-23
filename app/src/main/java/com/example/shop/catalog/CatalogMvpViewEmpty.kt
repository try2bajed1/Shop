package com.example.shop.catalog

import com.example.shop.catalog.CatalogMvpView
import com.example.shop.model.KOrder
import java.util.*

class CatalogMvpViewEmpty : CatalogMvpView {
    override fun setCatalogData(catalogList: MutableList<BaseCatalogItem>?) {
    }

    override fun getAnalyticName(): String =""

    override fun showWaitPrintingDialog(){}

    override fun showWaitCardProcessingDialog(){}

    override fun showWaitDialogWithText(msg: String){}

    override fun hideWaitDialog(){}

    override fun hideDialog(){}

    override fun showErrToast(msg: String?){}

    override fun showSuccToast(msg: String){}

    override fun showWarningAlert(text: String?){}

    override fun setWaitMessage(msg: String){}

    override fun setGridPresentationMode() {}

    override fun setListPresentationMode() {}

    override fun showEmptyLayout(){}

    override fun showNonEmptyLayout(){}

    override fun navigate2Basket(){}

    override fun navigate2Calc(){}

    override fun enableNavUpBtn(b: Boolean){}

    override fun showDefaultTitle(){}

    override fun updateTitle(string: String){}

    override fun stopRefresh(success: Boolean, errorText: String?){}

    override fun showAddModificationsDialog(list: ArrayList<VariantAdapterItem>){}

    override fun showWeightAmountDialogAdd(variant: Variant){}

    override fun update(order: KOrder){}

    override fun showTotalBar(isPortrait: Boolean){}

    override fun processOversellingError(){}

    override fun processSuccessScanSound(){}

    override fun showBlueTopSnackbar(){}

    override fun doesCreateContratorDialogExist() = false

    override fun showDetailedDialog(){}

    override fun updateCatalog(){}

    override fun onRefresh(){}
}