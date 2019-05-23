package com.example.shop.catalog

import com.example.shop.db.CatalogRepo

/**
 * Класс для навигации по каталогу
 */
class HistoryHolder(private val catalogRepo: CatalogRepo, category_id: Int, private val maxSpansCount: Int) {

    companion object {
        //Сколько записей из базы данных грузить за 1 раз
        private const val PRODUCTS_ON_PAGE = 30L
    }

    //Идентификатор текущей категории
    val categoryId: Int = catalogRepo.passThrough(category_id)
    //Список подкатегорий
    private val holdCategories: MutableList<Category> = mutableListOf()
    private val categories: MutableList<Category>
        get() {
            if (holdCategories.isEmpty())
                holdCategories.addAll(catalogRepo.getSubCategories(categoryId, maxSpansCount))
            return holdCategories
        }

    //Список продуктов в категории
    private val holdProducts: MutableList<Product> = mutableListOf()
    private val products: MutableList<Product>
        get() {
            if (holdProducts.isEmpty()) next()
            return holdProducts
        }
    //Заголовок текущей категории
    val title: String
        get() = catalogRepo.getTitle(categoryId) ?: ""
    //true, если загрузка продуктов и категорий текущей категории закончена
    private var isOver: Boolean = false

    private var offset: Long = 0

    /**
     * Возвращает список категорий и продуктов
     * @param refresh - true, если требуется заново загрузить контент категории из базы
     */
    fun get(refresh: Boolean): List<BaseCatalogItem> {
        if (refresh) {
            offset = 0
            isOver = false
            this.holdProducts.clear()
            this.holdCategories.clear()
        }
        return categories + products
    }

    fun atEnd(): Boolean = isOver

    /**
     * Возвращает следующие загруженные продукты
     */
    fun next(): MutableList<Product> =
            offset.takeIf { !isOver }?.let {
                val productsToAdd = getFilteredNext()
                holdProducts.addAll(productsToAdd)
                productsToAdd
            } ?: mutableListOf()

    /**
     * Возвращает отфильтрованные продукты в количестве [PRODUCTS_ON_PAGE] или меньше
     * Если в отфильтрованном списке меньше продуктов, чем в исходном, то запрашивает
     */
    private fun getFilteredNext(amount: Int = 0): ArrayList<Product> = ArrayList<Product>().apply {
        val forFilter = catalogRepo.getSubProducts(categoryId, PRODUCTS_ON_PAGE, offset)
        val filtered = filterAdd(forFilter)
        isOver = forFilter.isEmpty()
        this.addAll(filtered)
        offset += PRODUCTS_ON_PAGE
        if (forFilter.size != filtered.size && amount + this.size < PRODUCTS_ON_PAGE)
            this.addAll(getFilteredNext(amount + this.size))
    }

    /**
     * Возвращает список элементов из [listProducts] в соответствии с фильтрами
     * Если нет фильтров - показываем все продукты
     */
    private fun filterAdd(listProducts: List<Product>) =
            ArrayList<Product>().apply { addAll(listProducts.availableProductsToShow()) }
}