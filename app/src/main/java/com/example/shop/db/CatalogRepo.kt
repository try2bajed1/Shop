package com.example.shop.db

import java.sql.SQLException
import java.util.*
import java.util.prefs.Preferences

/**
 * Created with IntelliJ IDEA.
 * User: nick
 * Date: 18/04/2019
 * Time: 14:01
 */
class CatalogRepo(val databaseHelper: DatabaseHelper) {

    val categoryDao
        get() = databaseHelper.categoriesDao

    val productDao
        get() = databaseHelper.productsDao

    val productImageDao
        get() = databaseHelper.productImagesDao

    val variantDao
        get() = databaseHelper.variantsDao


    val FIELD_POSITION = "position"
    val FIELD_CATEGORY_ID = "category_id"
    val FIELD_ARCHIVED_AT = "archived_at"
    val FIELD_TITLE = "title"
    val DEFAULT_ROOT_CATEGORY_ID = -1
    private val SHOW_ALL_CATEGORIES_EVEN_EMPTY = true // false; // не заниматься сворачиванием категорий


    fun getCategoryName(categoryId: Int): String =
            categoryDao.queryForEq(FIELD_ID, categoryId).firstOrNull()?.title
                    ?: categoryId.toString()

    private fun getCategoriesForCategory(id: Int): List<BaseCatalogItem> {
        try {
            val builder = categoryDao.queryBuilder()
            return if (id == DEFAULT_ROOT_CATEGORY_ID) {
                builder.where().isNull("archived_at").and().isNull("parent_id").query()
            } else {
                builder.where().isNull("archived_at").and().eq("parent_id", id).query()
            }
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    private fun getProductsForCategory(id: Int, limit: Long, offset: Long): List<BaseCatalogItem> {
        try {
            val builder = productDao.queryBuilder()
            if (offset >= 0) {
                builder.offset(offset)
            }
            if (limit >= 0) {
                builder.limit(limit)
            }
            val where = builder.where().isNull("archived_at")
            return if (id == DEFAULT_ROOT_CATEGORY_ID) {
                where.and().isNull("category_id").query()
            } else {
                where.and().eq("category_id", id).query()
            }
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    private fun isEmptyCategory(category: Category): Boolean { // если в категории есть только вложенные категории, но нет продуктов
        if (SHOW_ALL_CATEGORIES_EVEN_EMPTY) {
            return false
        }
        if (!getProductsForCategory(category.getId()!!, 1, -1).isEmpty()) {
            return false
        }
        // далее рекурсивно вызываем для каждой из вложенных категорий
        val categories = getCategoriesForCategory(category.id!!)
        for (c in categories) {
            if (!isEmptyCategory(c as Category)) {
                return false
            }
        }
        return true
    }

    private fun queryForSubCategories(parent_id: Int, limit: Long, offset: Long): MutableList<Category> {
        try {
            val builder = categoryDao.queryBuilder()
            if (limit >= 0) {
                builder.limit(limit)
            }
            if (offset > 0) {
                builder.offset(offset)
            }
            builder.orderBy(FIELD_POSITION, true)
            builder.orderBy(FIELD_TITLE, true)
            val where = builder.where().isNull("archived_at")

            return if (parent_id == DEFAULT_ROOT_CATEGORY_ID) {
                where.and().isNull("parent_id").query()
            } else {
                where.and().eq("parent_id", parent_id).query()
            }
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    fun passThrough(category_id: Int): Int { // если эта категория имеет только одну непустую категорию
        if (!getProductsInCategory(category_id, 1, 0).isEmpty()) {
            return category_id
        }

        return queryForSubCategories(category_id, -1, 0)
                .filter { !isEmptyCategory(it) }
                .let {
                    if (it.size == 1) passThrough(it[0].id!!)
                    else category_id
                }
    }


    fun getSubCategories(category_id: Int, MAX_SPANS_COUNT: Int): List<Category> {

        val list = queryForSubCategories(category_id, -1, 0)
        if (!SHOW_ALL_CATEGORIES_EVEN_EMPTY) {
            var i = list.size
            while (--i >= 0) {
                if (isEmptyCategory(list[i])) {
                    list.removeAt(i)
                }
            }
        }

        if (Preferences.getInstance().presentationMode == Preferences.CATALOG_AS_GRID) {
            val itemsToAdd: Int
            val size = list.size
            if (size < MAX_SPANS_COUNT && size != 0) {
                itemsToAdd = MAX_SPANS_COUNT - size
            } else {
                val rest = size % MAX_SPANS_COUNT
                itemsToAdd = if (rest == 0) 0 else MAX_SPANS_COUNT - rest
            }

            for (i in 0 until itemsToAdd) {
                val category = Category()
                category.setEmptyHolderTrue()
                list.add(category)
            }
        }
        return list
    }


    fun getSubProducts(category_id: Int, limit: Long, offset: Long): List<Product> {
        return getProductsInCategory(category_id, limit, offset)
    }

    fun getTitle(category_id: Int): String? {
        if (category_id == DEFAULT_ROOT_CATEGORY_ID) {
            return "Каталог"
        }
        val category = categoryDao.queryForId(category_id)
        return if (category != null) category!!.getTitle() else null
    }


    fun searchProducts(pattern: String, limit: Long, offset: Long): List<Product> {
        try {
            val builder = productDao.queryBuilder()
            if (limit >= 0) {
                builder.limit(limit)
            }
            if (offset > 0) {
                builder.offset(offset)
            }
            builder.orderBy(FIELD_POSITION, true)
            builder.orderBy("titleUpper", true) // надо ли сортировать при поиске? уточнить
            val where = builder.where().isNull("archived_at")
            where.and().not().eq("invisible", true).and().like("titleUpper", "%" + pattern.toUpperCase() + "%").prepare()
            return builder.query()
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    fun getProductsInCategory(category_id: Int, limit: Long, offset: Long): List<Product> {
        try {
            val builder = productDao.queryBuilder()
            if (limit >= 0) {
                builder.limit(limit)
            }
            if (offset > 0) {
                builder.offset(offset)
            }
            builder.orderBy(FIELD_POSITION, true)
            builder.orderBy("titleUpper", true)
            val where = builder.where().isNull("archived_at")
            return if (category_id == DEFAULT_ROOT_CATEGORY_ID) {
                where.and().isNull("category_id").and().not().eq("invisible", true).query()
            } else {
                where.and().eq("category_id", category_id).and().not().eq("invisible", true).query()
            }
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    fun queryAllCategories(): List<Category> { // список категорий для добавления товара
        try {
            return categoryDao.queryBuilder().where().isNull("archived_at")/*.and ( ).isNotNull ( "parent_id" )*/.query()
        } catch (x: Throwable) {
            x.printStackTrace()
            return ArrayList()
        }
    }


    fun saveCategories(set: Category.Set) {
        categoryDao.callBatchTasks {
            for (category in set.items)
                categoryDao.createOrUpdate(category)
        }
    }


    fun getVariantById(id: Int, fakeTitle: String = ""): Variant {
        return variantDao.queryForId(id) ?: Product(Variant(id)).apply { title = fakeTitle }.variantSingle
    }


    private fun deleteProduct(product: Product) {
        for (variant in product.variants)
            variantDao.delete(variant)
        for (image in product.images)
            productImageDao.delete(image)
        try {
            productDao.delete(product)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }


    fun saveProducts(set: Product.Set) {
        productDao.callBatchTasks {
            for (product in set.items)
                createProduct(product)
        }
    }


    fun createProduct(product: Product) {
        val prev = queryProductById(product.id)
        if (prev != null)
            deleteProduct(prev)
        product.titleUpper = getConcatedVariantsSearchFields(product) //  в это поле сваливаем все колонки дочерних вариантов для поиска в этой строке по лайку
        when (product.unit) {
            // унификация граммов и килограммов на всякий случай
            "psc" -> product.unit = "pce"
            "l" -> product.unit = "ltr"
            "kg" -> product.unit = "kgm"
            "g" -> product.unit = "grm"
        }
        try {
            productDao.create(product)
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        for (variant in product.variants) {
            variant.product = product
            variantDao.create(variant)
        }

        product.images?.let {
            for (image in it) {
                image.product = product
                productImageDao.createOrUpdate(image)
            }
        }

    }


    //бага в sqlite для кириллицы, надо все через upperCase , чтобы работал поиск
    // https://toster.ru/q/235600
    private fun getConcatedVariantsSearchFields(product: Product): String {
        val sb = StringBuilder(product.title.toUpperCase())
        for (variant in product.variants) {
            if (variant.hasBarcode()) {
                sb.append('_').append(variant.barcode)
            }
            if (variant.hasSku()) {
                sb.append('_').append(variant.sku)
            }
        }
        return sb.toString()
    }


    fun queryProductById(id: Int): Product? {
        try {
            return productDao.queryForId(id)
        } catch (e: SQLException) {
            e.printStackTrace()
            return null
        }

    }

}