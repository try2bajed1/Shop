package com.example.shop.catalog

import android.util.Log.w
import com.example.shop.app.AppSingleton
import com.example.shop.utils.RxBus
import com.example.shop.db.CatalogRepo
import com.example.shop.model.OrdersModel
import io.reactivex.disposables.CompositeDisposable
import com.example.shop.mvp.BasePresenter
import java.math.BigDecimal
import java.util.*
import java.util.prefs.Preferences
import kotlin.Comparator

class CatalogPresenter(view: CatalogMvpView,
                       private val preferences: Preferences,
                       private val ordersModel: OrdersModel,
                       private val isPortrait: Boolean,
                       private val maxSpansCount: Int) : BasePresenter<CatalogMvpView>(view) {

    private val catalogRepo = CatalogRepo(AppSingleton.INSTANCE.databaseHelper)

    private val catalogList = ArrayList<BaseCatalogItem>()
    private val historyStack = Stack<HistoryHolder>()
    private val compositeDisposable = CompositeDisposable()
    val isInRootCategory: Boolean get() = historyStack.size == 1
    val isHolderAtEnd: Boolean get() = historyStack.peek().atEnd()

    override fun onDestroy() {
        super.onDestroy()
        this.compositeDisposable.dispose()
    }

    fun getCurrentCategoryId() = historyStack.peek().categoryId

    /**
     * Переходит в корневой каталог
     */
    private fun showRootCatalog() {
        historyStack.clear()
        val rootId = preferences.catalogRootId
        historyStack.push(HistoryHolder(catalogRepo, rootId, this.maxSpansCount))
        openCurrentCategory(false)
        view.update(ordersModel.kOrder)
    }

    override fun onCreate() {
        super.onCreate()
        preferences.presentationMode = if (isPortrait) Preferences.CATALOG_AS_LIST else Preferences.CATALOG_AS_GRID
        this.subscribeOnRxBus()
        updatePresentationMode()
        showRootCatalog()
        view.onRefresh()
    }

    /**
     * Меняет вид списка в зависимости от [Preferences.getPresentationMode] и [isPortrait]
     */
    private fun updatePresentationMode() {
        if (preferences.presentationMode == Preferences.CATALOG_AS_GRID)
            view.setGridPresentationMode()
        else
            view.setListPresentationMode()

        view.showTotalBar(isPortrait)
    }

    fun onSelectByLongClick(product: Product) {
        ordersModel.selectedProductByLongClick = product
        view.showDetailedDialog()
    }

    /**
     * Запускает процесс добавления выбранного продукта в корзину
     * Если мы не даём добавить продукты с нулевым остатком,
     * то оставляем в списке только варианты с не нулевым остатком
     * Если у продукта после этого остался только один вариант, добавляем его в корзину
     * Иначе открываем диалог для выбора варианта продукта
     */
    fun productSelected(product: Product) =
            product.availableVariantsToShow().let {
                if (it.size == 1) {
                    product.availableVariantsForSellingMore(OrdersModel.instance.kOrder)
                            .getOrNull(0)
                            ?.let { variant -> processVariant(variant) }
                            ?: view.processOversellingError()
                } else view.showAddModificationsDialog(getAdapterArr(it))
            }

    /**
     * Добавляет продукт в корзину
     * Если продукт весовой и указано [quantity], то добавляем в корзину указанное количество товара
     * Если продукт штучный - добавляем в корзину 1 штуку
     * Если продукт весовой и не указано [quantity], то показываем диалог для указания веса
     * Иначе показываем диалог выбора количества
     */
    private fun addProductToBasket(variant: Variant, quantity: Double) {
        ordersModel.kOrder.let {
            it.add(variant, BigDecimal(quantity))
            RxBus.instanceOf().dispatchEvent(OrderChangedEvent(ordersModel.kOrder))
        }
    }

    /**
     * Открывает выбранную категорию в каталоге
     */
    fun categorySelected(category: Category) {
        category.id?.let {
            historyStack.push(HistoryHolder(catalogRepo, it, this.maxSpansCount))
            openCurrentCategory(false)
        }
    }

    /**
     * Возвращает каталог из текущей категории в предыдущую
     */
    fun navigateUp() {
        if (!this.isInRootCategory) {
            historyStack.pop()
            openCurrentCategory(true)
        }
    }

    /**
     * Подгружает следующую порцию товаров текущей категории, если положение скролла этого требует
     */
    fun processScroll(visibleItemCount: Int, totalItemCount: Int, pastVisibleItems: Int) {
        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
            catalogList.addAll(historyStack.peek().next())
            view.setCatalogData(catalogList)
        }
    }

    /**
     * Даёт [view] команду перейти в [CalcActivity], если текущий заказ не пустой
     */
    fun nav2Calc() {
        if (!ordersModel.kOrder.isEmpty()) {
            view.navigate2Calc()
        }
    }

    /**
     * Загружает в список элементы категории, находящейся на вершине [historyStack]
     * Даёт [view] команду перерисовать список и показать пустой layout
     * или список в зависимости от количества элементов в категории
     */
    private fun openCurrentCategory(refresh: Boolean) {
        catalogList.clear()
        catalogList.addAll(historyStack.peek().get(refresh))
        view.setCatalogData(catalogList)

        if (catalogList.isEmpty()) {
            view.showEmptyLayout()
        } else {
            view.showNonEmptyLayout()
        }

        this.applyCurrentToolbarState()
    }

    /**
     * Даёт [view] команду показать иконку-гамбургер или стрелку назад
     * в зависимости от того, находимся ли мы в корневой категории
     */
    private fun applyCurrentToolbarState() {
        if (this.isInRootCategory) {
            view.enableNavUpBtn(false)
            view.showDefaultTitle()
        } else {
            view.enableNavUpBtn(true)
            view.updateTitle(historyStack.peek().title)
        }
    }

    /**
     * Формирует список вариантов для [CatalogMvpView.showAddModificationsDialog]
     */
    private fun getAdapterArr(variants: Collection<Variant>): ArrayList<VariantAdapterItem> =
            ArrayList<VariantAdapterItem>().apply {
                variants.sortedWith(Comparator { variant1, variant2 ->
                    when {
                        variant1.orderWeight == variant2.orderWeight -> 0
                        variant1.orderWeight > variant2.orderWeight -> 1
                        variant1.orderWeight < variant2.orderWeight -> -1
                        else -> 0
                    }
                }).forEach {
                    it.product?.title?.takeIf { title -> this.find { mod -> mod.title == title } == null }
                            ?.let { title -> this.add(VariantAdapterItem(title = title)) }
                    this.add(VariantAdapterItem(it, it.getQuantityStr(0.0)))
                }
            }


    /**
     * Запускает процесс получения [Variant] по [barcodeScanned]
     */
    private fun processBarcodeEvent(barcodeScanned: BarcodeScanned) {
        if (BuildConfig.DEBUG)
            w(barcodeScanned.getBarcode())
        //сначала ищем по коду среди контрагентов, если там будет пусто, то ищем в вариантах
        val contractors = databaseHelper.searchContractors(barcodeScanned.getBarcode())
        when {
            contractors.isNotEmpty() -> SetDiscountByContractorUseCase().setDiscountByContractor(contractors[0])
            view.doesCreateContratorDialogExist() -> RxBus.instanceOf().dispatchEvent(CreateContractorEvent(barcodeScanned.getBarcode()))
            else -> {
                val barcodeVariantSubscription = GetVariantUseCase().getVariantByBarcode(barcodeScanned)
                        .setIOScheduler()
                        .subscribe(this::addScannedVariant)

                this.compositeDisposable.add(barcodeVariantSubscription)
            }
        }
    }

    /**
     * Запускает процесс добавления отсканированного товара в корзину
     */
    private fun addScannedVariant(variants: List<Variant>) {
        when (variants.size) {
            // товар не найден
            0 -> view.processOversellingError()
            // найден ровно один товар по баркоду
            1 -> {
                val variant = variants[0]
                if (variant.canSellMore(OrdersModel.instance.kOrder)) {
                    view.processSuccessScanSound()

                    variant.product?.let {
                        processVariant(variant)
                    }
                }
            }
            //У нескольких вариантов, принадлежащих к одному или нескольким товарам, одинаковый штрихкод
            else -> view.showAddModificationsDialog(getAdapterArr(variants))
        }
    }

    //если дробный , прокидываем
    private fun processVariant(variant: Variant) {
        when {
            variant.isLibra || variant.isLtr || variant.isFractional  -> view.showWeightAmountDialogAdd(variant)
            else -> addProductToBasket(variant, 1.0)
        }
    }

}