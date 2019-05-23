package com.example.shop.components

import android.content.Context
import android.graphics.PorterDuff
import android.support.annotation.ColorRes
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import com.example.shop.R


class InverseSpinnerAdapter(context: Context,
                            private val list: List<String>, private val big: Boolean = true,
                            @ColorRes private val darkColorRes: Int = R.color.blue,
                            @ColorRes private val lightColorRes: Int = R.color.white) : ArrayAdapter<String>(context, 0, list), SpinnerAdapter {

    private val darkColor by lazy { ContextCompat.getColor(context, darkColorRes) }
    private val lightColor by lazy { ContextCompat.getColor(context, lightColorRes) }

    private fun getTopLayout() = if (big) R.layout.item_spinner_inverse_big else R.layout.item_spinner_inverse_small

    private fun getNormalLayout() = if (big) R.layout.item_spinner_inverse_dropdown_big else R.layout.item_spinner_inverse_dropdown_small

    override fun getItem(position: Int): String? = list.getOrNull(position)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = buildView(getNormalLayout(), position, convertView, parent, true)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = buildView(getTopLayout(), position, convertView, parent, false)

    private fun buildView(@LayoutRes layout: Int, position: Int, convertView: View?, parent: ViewGroup, itIsDropDownView: Boolean) =
            (convertView ?: LayoutInflater.from(context).inflate(layout, parent, false)).apply {
                item_text.compoundDrawables.forEach { it?.setColorFilter(darkColor, PorterDuff.Mode.SRC_ATOP) }
                item_text.text = list.getOrNull(position)
                item_text.setTextColor(if (itIsDropDownView) lightColor else darkColor)
                item_text.setBackgroundColor(if (itIsDropDownView) darkColor else lightColor)
            }
}