package com.example.shop.components

import android.graphics.Color
import android.support.annotation.ColorRes
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.example.shop.R

/**
 * [Snackbar] с белым текстом для вывода уведомлений поверх экрана сверху
 */
class CustomSnackbar {

    companion object {
        /**
         * Возвращает голубой [Snackbar] с белым текстом
         * @param mainText - текст уведомления
         * @param actionText - текст для кнопки действия
         * @param container - layout, поверх которого требуется вывести уведомление
         * @param action - функция, которая выполняется при нажатии на кнопку действия
         * @return стандартный [Snackbar], для показа которого требуется вызвать [Snackbar.show]
         */
        fun makeBlueTopSnackbar(mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any): Snackbar =
                makeTopSnackbar(R.color.blue, mainText, actionText, container, action)

        /**
         * Возвращает красный [Snackbar] с белым текстом
         * @param mainText - текст уведомления
         * @param actionText - текст для кнопки действия
         * @param container - layout, поверх которого требуется вывести уведомление
         * @param action - функция, которая выполняется при нажатии на кнопку действия
         * @return стандартный [Snackbar], для показа которого требуется вызвать [Snackbar.show]
         */
        fun makeRedTopSnackbar(mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any): Snackbar =
                makeTopSnackbar(R.color.toast_red, mainText, actionText, container, action)

        /**
         * Возвращает красный [Snackbar] с белым текстом
         * @param mainText - текст уведомления
         * @param actionText - текст для кнопки действия
         * @param container - layout, поверх которого требуется вывести уведомление
         * @param action - функция, которая выполняется при нажатии на кнопку действия
         * @return стандартный [Snackbar], для показа которого требуется вызвать [Snackbar.show]
         */
        fun makeRedBottomSnackbar(mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any): Snackbar =
                makeBottomSnackbar(R.color.toast_red, mainText, actionText, container, action)

        private fun makeBottomSnackbar(@ColorRes color: Int, mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any) =
                makeSnackbar(color, mainText, actionText, container, action, Gravity.BOTTOM)


        private fun makeTopSnackbar(@ColorRes color: Int, mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any) =
                makeSnackbar(color, mainText, actionText, container, action, Gravity.TOP)


        private fun makeSnackbar(@ColorRes color: Int, mainText: String, actionText: String, container: CoordinatorLayout, action: () -> Any, gravity: Int) =
                Snackbar.make(container, mainText, Snackbar.LENGTH_INDEFINITE).apply {
                    this.setAction(actionText) {
                        action.invoke()
                        this.dismiss()
                    }
                    (this.view as Snackbar.SnackbarLayout).let {
                        val textView = it.findViewById(android.support.design.R.id.snackbar_text) as? TextView
                        textView?.maxLines = 2
                        it.setBackgroundColor(ContextCompat.getColor(this.context, color))
                        it.layoutParams = (it.layoutParams as CoordinatorLayout.LayoutParams).apply {
                            this.topMargin = context.resources.getDimension(R.dimen.top_snackbar_margin_top).toInt()
                            this.gravity = gravity or Gravity.CENTER_HORIZONTAL
                        }
                        it.findViewById<TextView>(android.support.design.R.id.snackbar_text).let {
                            it.setTextColor(Color.WHITE)
                            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.context.resources.getDimension(R.dimen.top_snackbar_text_size))
                        }
                        ViewCompat.setElevation(it, this.context.resources.getDimension(R.dimen.top_snackbar_elevation))
                    }
                    this.setActionTextColor(Color.WHITE)
                }
    }
}